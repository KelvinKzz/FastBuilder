package com.gaoding.fastbuild.cli.utils;

import com.gaoding.fastbuilder.lib.utils.FileUtil;
import com.gaoding.fastbuilder.lib.utils.Log;
import com.gaoding.fastbuilder.lib.utils.MD5Util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class Aapt2ValueHelper {

    private Set<String> mFilePath = new HashSet<>();

    public void parserXml(String path) {
        try {
            mFilePath.add(path);
            parserXmlIn(path);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void changeXml() {
        String res = BuildUtils.getResourcesPath() + "/apk/res/values";
        String newRes = BuildUtils.getResourcesPath() + "/apk/new_res/values";
        FileUtil.ensumeDir(new File(newRes));
        if (FileUtil.dirExists(res)) {
            File resFile = new File(res);
            for (File file : resFile.listFiles()) {//修改原来的值
                try {
                    changeXml(file.getAbsolutePath(), newRes + "/" + file.getName(), true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        for (String path : mFilePath) {//新增的值
            File file = new File(path);
            try {
                changeXml(file.getAbsolutePath(), newRes + "/new_" + MD5Util.getMd5(file.getAbsolutePath()) + ".xml", false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private Map<String, Node> mUpdateData = new HashMap<>();
    private Set<String> mWipeData = new HashSet<>();

    private List<String> mPathList = new ArrayList<>();

    public List<String> getPathList() {
        return mPathList;
    }

    //判断需要修改或删除
    private void parserXmlIn(String path) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new File(path));
        Element root = document.getDocumentElement();
        NodeList nodeList = root.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Node attribute = node.getAttributes().getNamedItem("name");
                if (checkNeedUpdate(node)) {
                    mUpdateData.put(node.getNodeName() + "@" + attribute.getNodeValue(), node);
                } else {
                    mWipeData.add(node.getNodeName() + "@" + attribute.getNodeValue());
                }
            }
        }
        Log.i("解析完毕 " + path);
    }

    public void parserXmlLayout(String path) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new File(path));
        document.normalizeDocument();
        Element root = document.getDocumentElement();
        if (!root.getNodeName().equals("layout")) {
            mPathList.add(path);
            return;
        }

        String fileName;
        int index = path.lastIndexOf("/");
        if (index != -1) {
            fileName = path.substring(index + 1, path.length() - 4);
        } else {
            fileName = path.substring(0, path.length() - 4);
        }

        NamedNodeMap namedNodeMap = root.getAttributes();
        Node realFirstNode = null;
        NodeList nodeList = root.getChildNodes();
        List<String> databindingVariableName = new ArrayList<>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeName().equals("data")) {
                NodeList nodeChildList = node.getChildNodes();
                for (int j = 0; j < nodeChildList.getLength(); j++) {
                    Node childNode = nodeChildList.item(j);
                    if (childNode.hasAttributes()) {
                        databindingVariableName.add(childNode.getAttributes().getNamedItem("name").getNodeValue());
                    }
                }
                root.removeChild(node);
            } else {
                if (!node.getNodeName().equals("#text") && realFirstNode == null) {
                    realFirstNode = node;
                    for (int j = 0; j < namedNodeMap.getLength(); j++) {
                        realFirstNode.getAttributes().setNamedItem(namedNodeMap.item(j).cloneNode(false));
                    }
                }
                parserXmlNode(node, new OnXmlParserListener() {
                    @Override
                    public void handleNode(Node node) {
                        NamedNodeMap attList = node.getAttributes();
                        if (attList != null) {
                            for (int i = 0; i < attList.getLength(); i++) {
                                Node attribute = attList.item(i);
                                for (String variableName : databindingVariableName) {
                                    if (attribute.getNodeValue().contains("@{" + variableName + ".")) {
                                        attList.removeNamedItem(attribute.getNodeName());
                                    }
                                }
                            }
                        }
                    }
                });
            }
        }
        String newLayoutDir = BuildUtils.getResourcesPath() + "/apk/handle_layout/layout";
        FileUtil.ensumeDir(newLayoutDir);
        String newPath = newLayoutDir + File.separator + fileName + ".xml";
        DocumentBuilderFactory factory1 = DocumentBuilderFactory.newInstance();
        factory1.setNamespaceAware(true);
        Document newDocument = factory1.newDocumentBuilder().newDocument();
        newDocument.setXmlStandalone(false);
        newDocument.appendChild(newDocument.adoptNode(realFirstNode.cloneNode(true)));
        newDocument.getDocumentElement().setAttribute("android:tag", "layout/" + fileName + "_0");
        Log.i("Layout处理完毕 " + newPath);
        writeFile(newDocument, newPath);
        mPathList.add(newPath);
    }

    public interface OnXmlParserListener {
        void handleNode(Node node);
    }

    private void parserXmlNode(Node node, OnXmlParserListener listener) {
        if (node == null) {
            return;
        }
        if (node.hasChildNodes()) {
            NodeList nodeChildList = node.getChildNodes();
            for (int i = 0; i < nodeChildList.getLength(); i++) {
                Node childNode = nodeChildList.item(i);
                parserXmlNode(childNode, listener);
            }
        } else {
            listener.handleNode(node);
        }
    }

    /**
     * 去重不支持的数据标签
     * @param item
     * @return
     */
    private boolean checkNeedUpdate(Node item) {
        if (item != null && item.getChildNodes() != null && item.getChildNodes().getLength() > 0) {
            NodeList nodeList = item.getChildNodes();
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if ("xliff:g".equals(node.getNodeName())) {
                    return false;
                }
            }
        }
        if (item != null && item.getAttributes() != null && item.getAttributes().getLength() > 0) {
            NamedNodeMap attList = item.getAttributes();
            for (int i = 0; i < attList.getLength(); i++) {
                Node attribute = attList.item(i);
                if (attribute.getNodeName() != null && attribute.getNodeName().contains("tools")) {
                    return false;
                }
            }
        }
        return true;
    }

    public void changeXml(String path, String newPath, boolean isUpdate) throws Exception {
        boolean hasChange = false;
        File file = new File(path);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(file);
        Element root = document.getDocumentElement();
        root.removeAttribute("xmlns:xliff");//处理xmlns:xliff  Xliff是XML Localization Interchange File Format
        root.removeAttribute("xmlns:tools");//处理xmlns:tools
        NodeList nodeList = root.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Node attribute = node.getAttributes().getNamedItem("name");

                String key = node.getNodeName() + "@" + attribute.getNodeValue();
                if (isUpdate) {
                    Node newNode = mUpdateData.get(key);
                    if (newNode != null) {
                        Log.i("修改：" + file.getName() + " " + key);
                        root.removeChild(node);
                        mWipeData.add(key);
                        root.appendChild(document.importNode(newNode, true));
                        hasChange = true;
                    }
                } else {
                    if (mWipeData.contains(key)) {
                        Log.i("移除：" + file.getName() + " " + key);
                        root.removeChild(node);
                        hasChange = true;
                    }
                }
            }
        }
        if (hasChange) {
            writeFile(document, newPath);
            mPathList.add(newPath);

            if (!isUpdate) {
                try {
                    rewrite(newPath);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Log.i("修改完毕" + path);
        }
    }

    private void rewrite(String newPath) {
        List<String> lines = FileUtil.getStrings(newPath);
        List<String> newLines = new ArrayList<>();
        for (String l : lines) {
            if (!l.trim().startsWith("//") && !l.trim().equals("")) {//去掉注释
                newLines.add(l);
            }
        }
        FileUtil.writeFile(newLines, newPath);
    }

    /**
     * 写入文件
     */
    private void writeFile(Document document, String path) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "utf-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        PrintWriter printWriter = new PrintWriter(new FileOutputStream(path));
        StreamResult result = new StreamResult(printWriter);
        DOMSource source = new DOMSource(document);
        transformer.transform(source, result);
        printWriter.flush();
    }

    /**
     * 检测public.txt是否存在，若不存在则自动生成
     */
    public static void checkAndGeneratePublicTxt() {
        File publicTxtFile = new File(BuildUtils.getResourcesPath() + "/public.txt");
        if (publicTxtFile.exists()) {
            return;
        }
        RFile2publicTxt(BuildUtils.getResourcesPath() + "/R.txt", publicTxtFile.getPath());
    }

    /**
     * R文件转换成public.txt
     *
     * R.txt格式（int type name id）或者（int[] styleable name {id,id,xxxx}）
     * public.txt格式（applicationId:type/name = id）
     *
     * int styleable ActionBar_background 0
     * 其中type为styleable需要过滤掉
     *
     * int style Widget_MaterialComponents_Tooltip 0x7f1002d0 --> applicationId:style/Widget.MaterialComponents.Tooltip = 0x7f1002d0
     * 为style需要进行特殊处理
     */
    public static void RFile2publicTxt(String RFilePath, String publicTextFilePath) {
        File RFile = new File(RFilePath);
        File publicTxtFile = new File(publicTextFilePath);
        if (!RFile.exists()) {
            return;
        }
        if (publicTxtFile.exists()) {
            return;
        }
        Log.i("根据R文件 生成 public.txt");

        List<String> sortedLines = new ArrayList<>();
        InputStream is;
        BufferedReader reader = null;
        try {
            is = new FileInputStream(RFile);

            String line;
            reader = new BufferedReader(new InputStreamReader(is));
            line = reader.readLine();
            while (line != null) {
                String[] test = line.split(" ");
                String type = test[1];
                String name = test[2];
                String idValue = test[3];
                if (!"styleable".equals(type)) {
                    if ("style".equals(type)) {
                        name = name.replace("_", ".");
                    }
                    sortedLines.add(String.format(Locale.US, "%s:%s/%s = %s", BuildUtils.getPackageName(), type, name, idValue));
                }
                line = reader.readLine();
            }
            Collections.sort(sortedLines);
            publicTxtFile.createNewFile();
            FileUtil.writeFile(sortedLines, publicTxtFile.getPath());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            FileUtil.safeClose(reader);
        }
    }

//    /**
//     * public.xml转换成public.txt
//     */
//    public static void publicXml2publicTxt() {
//        File publicXmlFile = new File(BuildUtils.getResourcesPath() + "/apk/res/values/public.xml");
//        File publicTxtFile = new File(BuildUtils.getResourcesPath() + "/public.txt");
//        if (!publicXmlFile.exists()) {
//            return;
//        }
//        Log.i("public.xml 生成 public.txt");
//        List<String> sortedLines = new ArrayList<>();
//    }

}
