package com.team.corporate;

import com.team.corporate.controllers.dialog.ViewType;
import com.team.corporate.controllers.navigation.Page;
import javafx.fxml.FXML;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class FxmlContractTest {
    private static final String FX = "http://javafx.com/fxml/1";

    static Stream<Path> views() throws Exception {
        try (Stream<Path> paths = Files.walk(Path.of("src/main/resources/fxml"))) {
            return paths.filter(path -> path.toString().endsWith(".fxml")).sorted().toList().stream();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("views")
    void controllersFieldsAndCallbacksMatchFxml(Path path) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        var document = factory.newDocumentBuilder().parse(path.toFile());
        var controller = Class.forName(document.getDocumentElement().getAttributeNS(FX, "controller"), false,
                getClass().getClassLoader());
        Map<String, Element> elementsById = new HashMap<>();
        Map<String, String> imports = new HashMap<>();
        for (var child = document.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == org.w3c.dom.Node.PROCESSING_INSTRUCTION_NODE && child.getNodeName().equals("import")) {
                String imported = child.getNodeValue().strip();
                imports.put(imported.substring(imported.lastIndexOf('.') + 1), imported);
            }
        }
        var elements = document.getElementsByTagName("*");
        for (int i = 0; i < elements.getLength(); i++) {
            var element = (Element) elements.item(i);
            String id = element.getAttributeNS(FX, "id");
            if (!id.isEmpty()) assertNull(elementsById.put(id, element), "Duplicate fx:id: " + id);
            for (int j = 0; j < element.getAttributes().getLength(); j++) {
                var attribute = element.getAttributes().item(j);
                if (attribute.getNodeName().startsWith("on") && attribute.getNodeValue().startsWith("#")) {
                    assertCallback(controller, attribute.getNodeValue().substring(1));
                }
            }
            if (element.getTagName().equals("Button")) {
                assertTrue(element.getAttribute("onAction").startsWith("#"), "Button has no callback: " + id);
            }
        }
        for (Class<?> type = controller; type != null; type = type.getSuperclass()) {
            for (var field : type.getDeclaredFields()) {
                if (!field.isAnnotationPresent(FXML.class)) continue;
                Element element = elementsById.get(field.getName());
                assertNotNull(element, "Missing fx:id for " + type.getName() + "." + field.getName());
                String nodeClass = imports.getOrDefault(element.getTagName(), element.getTagName());
                Class<?> nodeType = Class.forName(nodeClass, false, getClass().getClassLoader());
                assertTrue(field.getType().isAssignableFrom(nodeType), "FXML type does not match field: " + field.getName());
            }
        }
    }

    private void assertCallback(Class<?> controller, String name) {
        for (Class<?> type = controller; type != null; type = type.getSuperclass()) {
            for (var method : type.getDeclaredMethods()) {
                if (!method.getName().equals(name)) continue;
                if (!Modifier.isPublic(method.getModifiers()) && !method.isAnnotationPresent(FXML.class)) continue;
                if (method.getParameterCount() == 0 || method.getParameterCount() == 1
                        && javafx.event.Event.class.isAssignableFrom(method.getParameterTypes()[0])) return;
            }
        }
        fail("Missing FXML callback: " + controller.getName() + "." + name);
    }

    @Test
    void everyRegisteredPageAndDialogExists() {
        for (Page page : Page.values()) assertNotNull(getClass().getResource(page.getFxmlPath()), page.name());
        for (ViewType view : ViewType.values()) assertNotNull(getClass().getResource(view.getFxmlPath()), view.name());
    }
}
