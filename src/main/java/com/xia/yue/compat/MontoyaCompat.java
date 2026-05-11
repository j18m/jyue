package com.xia.yue.compat;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpMessage;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import com.xia.yue.burp.CapturedExchange;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class MontoyaCompat {
    private MontoyaCompat() {
    }

    public static String pathWithoutQuery(HttpRequest request) {
        String path = invokeStringNoArg(request, "pathWithoutQuery");
        if (path == null || path.isBlank()) {
            path = request.path();
        }
        return stripQuery(path);
    }

    public static HttpRequestResponse sendRequest(MontoyaApi api, HttpRequest request, long responseTimeoutMillis) {
        HttpRequestResponse response = sendRequestWithOptions(api, request, responseTimeoutMillis);
        return response == null ? api.http().sendRequest(request) : response;
    }

    public static String mimeType(HttpResponse response) {
        Object mimeType = invokeNoArg(response, "mimeType");
        if (mimeType != null) {
            return mimeType.toString();
        }
        mimeType = invokeNoArg(response, "statedMimeType");
        if (mimeType != null) {
            return mimeType.toString();
        }
        String contentType = headerValue(response, "Content-Type");
        if (contentType == null || contentType.isBlank()) {
            return "";
        }
        int semicolon = contentType.indexOf(';');
        return semicolon >= 0 ? contentType.substring(0, semicolon).trim() : contentType.trim();
    }

    public static CapturedExchange capturedExchange(HttpRequest request, HttpResponse response) {
        return new CapturedExchange(request, response);
    }

    public static HttpRequest emptyRequest() {
        Object request = invokeStaticNoArg(HttpRequest.class, "httpRequest");
        return request instanceof HttpRequest typed ? typed : null;
    }

    public static HttpResponse emptyResponse() {
        Object response = invokeStaticNoArg(HttpResponse.class, "httpResponse");
        return response instanceof HttpResponse typed ? typed : null;
    }

    public static String headerValue(HttpMessage message, String name) {
        try {
            return message.headerValue(name);
        } catch (Exception | LinkageError ignored) {
            return "";
        }
    }

    public static void logError(MontoyaApi api, String message, Throwable throwable) {
        try {
            Object logging = api.logging();
            Method method = logging.getClass().getMethod("logToError", String.class, Throwable.class);
            method.invoke(logging, message, throwable);
            return;
        } catch (Exception | LinkageError ignored) {
            // Fall through to the older PrintStream-style API.
        }

        try {
            api.logging().error().println(message);
            if (throwable != null) {
                throwable.printStackTrace(api.logging().error());
            }
        } catch (Exception | LinkageError ignored) {
            // Do not let logging compatibility break Burp callbacks.
        }
    }

    public static List<HttpRequestResponse> selectedRequestResponses(ContextMenuEvent event) {
        List<HttpRequestResponse> messages = new ArrayList<>();
        Object selected = invokeNoArg(event, "selectedRequestResponses");
        if (selected instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                if (item instanceof HttpRequestResponse requestResponse) {
                    messages.add(requestResponse);
                }
            }
        }
        Object editor = invokeNoArg(event, "messageEditorRequestResponse");
        if (editor instanceof Optional<?> optional) {
            optional.ifPresent(value -> addEditorMessage(messages, value));
        } else if (editor != null) {
            addEditorMessage(messages, editor);
        }
        return messages;
    }

    private static void addEditorMessage(List<HttpRequestResponse> messages, Object editor) {
        Object requestResponse = invokeNoArg(editor, "requestResponse");
        if (requestResponse instanceof HttpRequestResponse typed) {
            messages.add(typed);
        }
    }

    private static HttpRequestResponse sendRequestWithOptions(MontoyaApi api, HttpRequest request, long responseTimeoutMillis) {
        try {
            Class<?> optionsClass = Class.forName("burp.api.montoya.http.RequestOptions");
            Object options = optionsClass.getMethod("requestOptions").invoke(null);
            options = optionsClass.getMethod("withResponseTimeout", long.class).invoke(options, responseTimeoutMillis);
            Method sendRequest = api.http().getClass().getMethod("sendRequest", HttpRequest.class, optionsClass);
            Object response = sendRequest.invoke(api.http(), request, options);
            return response instanceof HttpRequestResponse requestResponse ? requestResponse : null;
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException | LinkageError ignored) {
            return null;
        }
    }

    private static String invokeStringNoArg(Object target, String methodName) {
        Object value = invokeNoArg(target, methodName);
        return value instanceof String string ? string : null;
    }

    private static Object invokeNoArg(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | LinkageError ignored) {
            return null;
        }
    }

    private static Object invokeStaticNoArg(Class<?> type, String methodName) {
        try {
            Method method = type.getMethod(methodName);
            return method.invoke(null);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | LinkageError ignored) {
            return null;
        }
    }

    private static String stripQuery(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        int queryIndex = path.indexOf('?');
        return queryIndex >= 0 ? path.substring(0, queryIndex) : path;
    }
}
