package code;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class AndreyTest {

	public static void main(String[] args) {
		//----Создание логического объекта XML-документа----//
		Document document = null;	
		
		try { 
			File script = new File("res/scripts/lecture.xml");	
			//фабрика парсеров XML
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			//Парсим документ
			document = dBuilder.parse(script);
			
			Element prepod = (Element) document.getElementsByTagName("Krivonosova").item(0);
			NodeList lectures = prepod.getElementsByTagName("lecture");
			for (int i = 0; i < lectures.getLength(); i++) {
				Element lect = (Element) lectures.item(i);
				NodeList replicas = lect.getElementsByTagName("replica");
				if (!lect.getAttribute("id").equals("2")) {
					System.out.println("---Лекция #"+i+"---");
					for (int j = 0; j < replicas.getLength(); j++) {
						Element repl = (Element) replicas.item(j);
						System.out.println(repl.getTextContent());
					}
				}
			}
		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
		}
	}
}
