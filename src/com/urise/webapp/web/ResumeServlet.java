package com.urise.webapp.web;

import com.urise.webapp.Config;
import com.urise.webapp.model.ContactType;
import com.urise.webapp.model.Resume;
import com.urise.webapp.storage.Storage;
import com.urise.webapp.util.Validator;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ResumeServlet extends HttpServlet {

    private Storage storage;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
        storage = Config.get().getStorage();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String uuid = req.getParameter("uuid");
        String action = req.getParameter("action");
        if (action == null) {
            req.setAttribute("resumes", storage.getAllSorted());
            req.getRequestDispatcher("/WEB-INF/jsp/list.jsp").forward(req, resp);
            return;
        }
        processAction(req, resp, uuid, action);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        req.setCharacterEncoding("UTF-8");
        String uuid = req.getParameter("uuid");
        String fullName = req.getParameter("fullName");
        List<String> validatingProblems = new ArrayList<>();
        if (fullName == null || fullName.trim().length() == 0) {
            validatingProblems.add("Name must not be empty");
        }
        Resume r;
        if (!uuid.equals("")) {
            r = storage.get(uuid);
            fillResume(req, r, fullName);
            storage.update(r);
        } else {
            r = new Resume(fullName);
            fillResume(req, r, fullName);
            storage.save(r);
        }
        validateResume(r, validatingProblems);
        if (validatingProblems.size() > 0) {
            req.setAttribute("problems", validatingProblems);
            req.setAttribute("resume", r);
            req.getRequestDispatcher("/WEB-INF/jsp/edit.jsp").forward(req, resp);
        }
        resp.sendRedirect("resume");
    }

    private void processAction(HttpServletRequest req, HttpServletResponse resp, String uuid, String action)
            throws IOException, ServletException {
        Resume r;
        switch (action) {
            case "delete":
                storage.delete(uuid);
                resp.sendRedirect("resume");
                return;
            case "view":
            case "edit":
                if (!uuid.equals("")) {
                    r = storage.get(uuid);
                } else {
                    r = new Resume();
                }
                break;
            default:
                throw new IllegalArgumentException("Action " + action + " is illegal");
        }
        req.setAttribute("resume", r);
        req.setAttribute("problems", new ArrayList<String>());
        req.getRequestDispatcher(
                ("view".equals(action) ? "/WEB-INF/jsp/view.jsp" : "/WEB-INF/jsp/edit.jsp")
        ).forward(req, resp);
    }

    private void fillResume(HttpServletRequest req, Resume r, String fullName) {
        r.setFullName(fullName);
        for (ContactType type : ContactType.values()) {
            String value = req.getParameter(type.name());
            if (value != null && value.trim().length() != 0) {
                r.setContact(type, value);
            } else {
                r.getContacts().remove(type);
            }
        }
    }

    private void validateResume(Resume resume, List<String> validatingProblems) {
        String phoneNumber = resume.getContact(ContactType.PHONE_NUMBER);
        validate(phoneNumber, Validator.validatePhoneNumber(phoneNumber),
                s -> validatingProblems.add("Phone number must be like this: 79009991122"));

        String email = resume.getContact(ContactType.EMAIL);
        validate(email, Validator.validateEmail(email),
                s -> validatingProblems.add("E-mail address is incorrect"));

        String linkedInProfile = resume.getContact(ContactType.LINKED_IN);
        validate(linkedInProfile, Validator.validateUrl(linkedInProfile, "linkedin.com"),
                s -> validatingProblems.add("Link to a LinkedIn profile is incorrect"));

        String gitProfile = resume.getContact(ContactType.GITHUB);
        validate(gitProfile, Validator.validateUrl(gitProfile, "github.com"),
                s -> validatingProblems.add("Link to a Github profile is incorrect"));

        String stackProfile = resume.getContact(ContactType.STACKOVERFLOW);
        validate(stackProfile, Validator.validateUrl(stackProfile, "stackoverflow.com"),
                s -> validatingProblems.add("Link to a StackOverflow profile is incorrect"));

        String homePage = resume.getContact(ContactType.HOME_PAGE);
        validate(homePage, Validator.validateUrl(homePage),
                s -> validatingProblems.add("Home page must be a valid URL"));

    }

    private void validate(String value, Boolean condition, Consumer<String> consumer) {
        if (!value.equals("") && (value.trim().length() == 0 || !condition)) {
            consumer.accept(value);
        }
    }
}
