package com.urise.webapp;

import com.urise.webapp.model.Resume;
import com.urise.webapp.storage.Storage;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MainReflection {

    public static void main(String[] args) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Resume resume = new Resume();
        Field field = resume.getClass().getDeclaredFields()[0];
        field.setAccessible(true);
        System.out.println(field.getName());
        System.out.println(resume);
        field.set(resume, "uuid1_test");
        System.out.println(invokeMethodFromResume(resume, "toString"));
        field.setAccessible(false);
    }

    public static String invokeMethodFromResume(Resume resume, String methodName) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = resume.getClass().getDeclaredMethod(methodName);
        return (String) method.invoke(resume);

    }

}