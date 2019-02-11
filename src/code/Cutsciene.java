package code;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.Timer;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.newdawn.slick.Animation;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.Music;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.Sound;
import org.newdawn.slick.SpriteSheet;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Класс катсцены.
 * <br>Позволяет создавать ролики из отдельных <i>изображений, анимаций, музыки, и звуков</i>
 * @version 0.4
 * */
public class Cutsciene {
	/*
	 * Заметка:
	 * [тому, кто собирается поддерживать этот класс]
	 * Был вариант сделать сделать интерфейс а-ля Resourse, отнаследовать все типы ресурсов
	 * от него в отдельных классах и уже там реализовать парсинг, рендеринг/воспроизведение и т.д.;
	 * Но автор сего в порядке эксперимента рискнул, и собрал всё это в один god-object, 
	 * и принимал последующие решения с точки зрения этого подхода.
	 **/
	
	/**Класс-структура изображения*/
	private class Frame{
		Image image;
		float start, end;
	}
	
	/**Класс-структура музыки*/
	private class Composition{
		Music music;
		float start, end;
	}
	
	/**Класс-структура звука*/
	private class SingleSound{
		Sound sound;
		float start;	
	}

	/**Класс-структура анимации*/
	private class Animated{
		Animation anim;
		float start, end;	
	}
	
	//скрипт-источник, может быть пуст
	String sourceScript = null;
	
	//"дорожки" ресурсов на таймлайне - изображения, музыка, звуки, и анимации соответственно
	private ArrayList<Frame> frameset = new ArrayList<>();
	private ArrayList<Composition> musicset = new ArrayList<>();
	private ArrayList<SingleSound> soundset = new ArrayList<>();
	private ArrayList<Animated> animset = new ArrayList<>();
	
	//Лейтмотив, как лаконичная альтернатива коллекции из одного файла
	private Music leithmotive = null;
	
	//секундомер с точностью до 0.1 секунды
	private float playingTime;
	private long startTimeMillis;

	private boolean isPlaying = false;//почти как клавиша "Play"
	
	/**Конструктор для катсцен с XML-сценарием
	 * @param filename путь к файлу сценария
	 * */
	public Cutsciene(String filename) {
		sourceScript = filename;
		parseScript(filename);
	}
	
	/**Конструктор для создания катсцены вручную, из кода программы*/
	public Cutsciene() {/*реализация не требуется*/}
	
	/**Отрисовка катсцены на экране
	 * @param g инструменты отрисовки из инструментария Slick2D
	 * @param x координата отрисовки по-горизонтали
	 * @param y координата отрисовки по-вертикали
	 * */
	public void render(Graphics g, float x, float y) {
		for(Frame frame: frameset) {
			if(playingTime > frame.start && playingTime < frame.end) g.drawImage(frame.image, 0, 0);
		}
		for(Animated animated: animset) {
			if(playingTime > animated.start && playingTime < animated.end) g.drawAnimation(animated.anim, 0, 0);
		}
	}
	
	public void update(int delta) {
		
	}
	
	/**Добавляет в катсцену изображение, отображающееся на экране заданный промежуток времени
	 * @param image - изображение
	 * @param start - время начала отрисовки(в сек.)
	 * @param end - время конца отрисовки(в сек.)
	 * */
	public void insertAt(Image image, float start, float end) {
        Frame frame = new Frame();
        frame.image = image;
        frame.start = start;
        frame.end = end;
        
        frameset.add(frame);
	}
	
	/**Добавляет новую музыкальную композицию в указанное место на таймлайне
	 * @param start время начала воспроизведения в сек(с момента начала катсцены)
	 * @param end момент (в сек) от начала катсцены, в который воспроизведение композиции прервётся
	 * */
	public void insertAt(Music soundtrack, float start, float end) {
		Composition composition = new Composition();
		composition.music = soundtrack;
		composition.start = start;
		composition.end = end;
		
		musicset.add(composition);
	}
	
	/**Добавляет новую музыкальную композицию в указанное место на таймлайне
	 * @param start время начала воспроизведения в сек(считается от начала дорожки)
	 * */
	public void insertAt(Music soundtrack, float start) {
		Composition composition = new Composition();
		composition.music = soundtrack;
		composition.start = start;
		composition.end = -1;
		
		musicset.add(composition);
	}
	
	/**Добавляет новый звук, играющий с нужного момента
	 * @param sound звук, который следует воспроизвести
	 * @param start время начала воспроизведения в сек(с начала катсцены)
	 * */
	public void insertAt(Sound sound, float start) {
		SingleSound singlesound = new SingleSound();
		singlesound.sound = sound;
		singlesound.start = start;
		
		soundset.add(singlesound);
	}
	
	/**
	 * Добавление покадровой анимации
	 * <br><i><b>[примечание]</b>все анимации созданные при помощи XML по-умолчанию не зациклены, 
	 * и если она закончится раньше, чем окончился рендеринг, то будет отрисовываться последний кадр</i>
	 * @param anim анимация, которую следует отрисовать
	 * @param start время начала анимации
	 * @param end время конца анимации
	 * */
	public void insertAt(Animation anim, float start, float end) {
		Animated animated = new Animated();
		animated.anim = anim;
		animated.start = start; 
		animated.end = end;
		
		animset.add(animated);
	}
	
	/**
	 * Исключает изображение из катсцены
	 * @param image объект, который следует исключить
	 * */
	public void remove(Image image) {
		for(Frame frame : frameset) 
			if(image.equals(frame.image)) frameset.remove(frame);
	}
	
	/**
	 * Исключает музыкальную композицию из катсцены
	 * @param music объект, который следует исключить
	 * */
	public void remove(Music music) {
		for(Composition soundtrack : musicset) 
			if(music.equals(soundtrack.music)) musicset.remove(soundtrack);
	}

	/**
	 * Исключает звуковую дорожку из катсцены
	 * @param music объект, который следует исключить
	 * */
	public void remove(Sound sound) {
		for(SingleSound soundtrack : soundset) 
			if(sound.equals(soundtrack.sound)) soundset.remove(soundtrack);
	}

	/**
	 * Исключает анимацию из катсцены
	 * @param music объект, который следует исключить
	 * */
	public void remove(Animation animation) {
		for(Animated animated: animset) 
			if(animation.equals(animated.anim)) soundset.remove(animated);
	}	
	
	/**Парсер XML-скрипта*/
	private void parseScript(String source) {
		Document document = null;	
		
		try { 
			//подготовка к генерации объекта при помощи фабрики
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			//генерируем document из XML
			document = dBuilder.parse(source);
		} catch (ParserConfigurationException | SAXException | IOException e) {
			System.err.println("XML-сценарий катсцены повреждён, или содержит ошибки. Детали: ");
			e.printStackTrace();
		}
		
		try {
			//--парсинг путей к ресурсам--//
			String src_music, src_frames, src_anim, src_sound;
			//при отсутствии явного определения путей, все ресурсы будут считаться находящимися в той же папке, что и исполняемый код 
			Element cutsciene = (Element)document.getElementsByTagName("cutsciene").item(0);
			src_music = cutsciene.hasAttribute("music") ? cutsciene.getAttribute("music") : "";
			src_sound = cutsciene.hasAttribute("sound") ? cutsciene.getAttribute("sound") : "";
			src_frames = cutsciene.hasAttribute("image") ? cutsciene.getAttribute("image") : "";
			src_anim = cutsciene.hasAttribute("animation") ? cutsciene.getAttribute("animation") : "";
			//-------------------------//
					
			//Парсинг изображений
			NodeList images = document.getElementsByTagName("image");
			for (int i = 0; i < images.getLength(); i++) {
				Element element = (Element)images.item(i);
				String src = src_frames + element.getAttribute("src");
				float start = Float.parseFloat(element.getAttribute("start"));
				float end = Float.parseFloat(element.getAttribute("end"));
				insertAt(new Image(src), start, end);
			}

			//Парсинг музыки
			NodeList music = document.getElementsByTagName("music");
			for (int i = 0; i < music.getLength(); i++) {
				Element element = (Element)images.item(i);
				String src = src_music + element.getAttribute("src");
				float start = Float.parseFloat(element.getAttribute("start"));
				if(!element.hasAttribute("end")) {
					float end = Float.parseFloat(element.getAttribute("end"));
					insertAt(new Music(src), start, end);
				}else 
					insertAt(new Music(src), start);
			}
			
			//Парсинг звука
			NodeList sounds = document.getElementsByTagName("sound");
			for (int i = 0; i < sounds.getLength(); i++) {
				Element element = (Element)images.item(i);
				String src = src_sound + element.getAttribute("src");
				float start = Float.parseFloat(element.getAttribute("start"));
				insertAt(new Sound(src), start);
			}
			
			//Парсинг анимации
			NodeList animations = document.getElementsByTagName("music");
			for (int i = 0; i < animations.getLength(); i++) {
				Element element = (Element)images.item(i);
				String src = src_anim + element.getAttribute("src");
				//начало и конец
				float start = Float.parseFloat(element.getAttribute("start"));
				float end = Float.parseFloat(element.getAttribute("end"));
				//размер кадра
				int frame_width = Integer.parseInt(element.getAttribute("frame_width"));
				int frame_height = Integer.parseInt(element.getAttribute("frame_height"));
				//частота кадров
				int framerate = Integer.parseInt(element.getAttribute("framerate"));
				//спрайт-лист
				SpriteSheet spriteSheet = new SpriteSheet(new Image(src), frame_width, frame_height);
				//анимация из спрайт-листа
				Animation anim = new Animation(spriteSheet, framerate);
				insertAt(anim, start, end);
			}
			
			//Парсинг лейтмотива
			NodeList leithmotiveNode = document.getElementsByTagName("leithmotive");
			Element element = (Element) leithmotiveNode.item(0);
			String src = element.getAttribute("src");
			
			leithmotive = new Music(src);
		} catch (SlickException e) {
			System.err.println("Ошибка парсинга! ");
			e.printStackTrace();
		}
		
	}	
	
	/**Запуск катсцены на воспроизведение.
	 * <br>Повторный вызов начнёт воспроизведение с начала ролика.
	 * */
	public void start() {		
		playingTime = 0;
		startTimeMillis = System.currentTimeMillis();
		if(leithmotive != null) leithmotive.loop();
		isPlaying = true;
	}
	
	/**Остановка воспроизведения*/
	public void stop() {
		isPlaying = false;
		if(leithmotive != null) leithmotive.stop();
	}
	
	/**Возвращает время, прошедшее с момента старта катсцены*/
	public float playingAt() {
		return playingTime;
	}
	
	private void sort() {}
	
	/**Возвращение катсцены в исходное состояние*/
	public void reset() {}
	
	/**Перезагрузка скрипта, и замещение старых данных новыми.
	 * <br> Если исходного скрипта не существует (например, если катсцена создана программно),
	 * то метод просто перезапустит ролик с начала
	 * */
	public void reload() {
		//если перезагружается работающая катсцена, то она временно останавливается
		boolean played = false;
		if(isPlaying) {
			stop();
			played = true;
		}
		
		//очистка дорожек и парсинг скрипта
		if(sourceScript != null) {
			soundset.clear();
			musicset.clear();
			frameset.clear();
			animset.clear();
			leithmotive = null;
			parseScript(sourceScript);
		}
		
		//после перезагрузки, работавшая катсцена начнётся с начала
		if(played) start();
	}
	
	 /* TODO: Вынести парсинг в отдельный поток, чтобы избежать фризов
	  Пока происходит парсинг, (не boolean loaded = true) в центр черного экрана должна выводиться небольшая белая надпись "loading..."*/
	 
	 //TODO: Сделать субтитры!!
}
