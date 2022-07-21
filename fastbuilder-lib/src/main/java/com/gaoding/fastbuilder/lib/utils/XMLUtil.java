package com.gaoding.fastbuilder.lib.utils;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class XMLUtil {

    //        def generate_id_file_by_public(public_path, ids_path):
//        if not os.path.exists(public_path):
//        raise FreelineException("public file not found", "public file path: {}".format(public_path))
//
//        tree = ET.ElementTree(ET.fromstring(remove_namespace(public_path)))
//        ids_root = ET.Element('resources')
//        for elem in tree.iterfind('public[@type="id"]'):
//        node = ET.SubElement(ids_root, "item")
//        node.attrib['name'] = elem.attrib['name']
//        node.attrib['type'] = "id"
//        ids_tree = ET.ElementTree(ids_root)
//        ids_tree.write(ids_path, encoding="utf-8")

    //    <?xml version="1.0" encoding="utf-8"?>
//<resources>
//<item type="id" name="playCount"/>
    public static void buildIds(String publicPath, String idsPath) throws Exception {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
        InputStream is = new FileInputStream(publicPath);
        Document document = builder.parse(is);
        Element rootElement = document.getDocumentElement();
        Log.i("rootElement:" + rootElement.getNodeName());
        NodeList nodeList = rootElement.getElementsByTagName("public");
        List<String> itemList = new ArrayList<>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element element = (Element) nodeList.item(i);
//            Log.i("type = " + element.getAttribute("type"));
            if ("id".equals(element.getAttribute("type"))) {
//                Log.i("name = " + element.getAttribute("name"));
                itemList.add(element.getAttribute("name"));
            }
        }

        saveToXML(idsPath, itemList);
    }

    private static void saveToXML(String idsPath, List<String> itemList) throws Exception {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();

        Document document = builder.newDocument();
        Element resources = document.createElement("resources");
        document.appendChild(resources);
        for (String name : itemList) {
            Element item = document.createElement("item");
            resources.appendChild(item);

            Attr type = document.createAttribute("type");
            type.setNodeValue("id");
            item.setAttributeNode(type);

            Attr attr = document.createAttribute("name");
            attr.setNodeValue(name);
            item.setAttributeNode(attr);
        }

        WriteFile(idsPath, document);
    }

    private static void WriteFile(String idsPath, Document document) throws Exception {
        // 声明文件流
        PrintWriter printWriter = new PrintWriter(new FileOutputStream(new File(idsPath)));
        StreamResult result = new StreamResult(printWriter);
        DOMSource source = new DOMSource(document);

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        // 设置转换中实际的输出属性
        // 指定首选的字符编码
        transformer.setOutputProperty(OutputKeys.ENCODING, "utf-8");
        // indent="yes"|"no".指定了当输出结果树时，Transformer是否可以添加额外的空白
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.transform(source, result);
        Log.i("生成XML文件成功!");
    }

}
