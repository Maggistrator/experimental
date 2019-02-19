package code;

import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.newdawn.slick.Animation;
import org.newdawn.slick.Color;
import org.newdawn.slick.Font;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.Music;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.Sound;
import org.newdawn.slick.SpriteSheet;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import core.TrueTypeFont;

/**
 * Класс катсцены.
 * <br>Позволяет создавать ролики из отдельных <i>изображений, анимаций, музыки, и звуков</i>
 * @version 0.4
 * */
public class Cutsciene {
	/*
	 * Заметка:
	 * Был вариант сделать сделать интерфейс а-ля Resourse, отнаследовать все типы ресурсов
	 * от него в отдельных классах и уже там реализовать парсинг, рендеринг/воспроизведение и т.д.; 
	 * {т.е. реализовать фабрику/фабричный метод} но автор сего в порядке эксперимента рискнул, 
	 * и собрал всё это в один god-object, и принимал последующие решения с точки зрения этого подхода.
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
		boolean played = false;
	}
	
	/**Класс-структура звука*/
	private class SingleSound{
		Sound sound;
		float start;
		boolean played = false;
	}

	/**Класс-структура анимации*/
	private class Animated{
		Animation anim;
		float start, end;	
	}

	/**Класс-структура субтитров*/
	private class Subtitle{
		float x, y;
		float width, height;
		float start, end;
		String text;
		String[] lines = {};
	}
	
	//скрипт-источник, может быть пуст
	String sourceScript = null;
	
	private float linewidthFactor = 0.2f;
	
	//"дорожки" ресурсов на таймлайне - изображения, музыка, звуки, и анимации соответственно
	private ArrayList<Frame> frameset = new ArrayList<>();
	private ArrayList<Composition> musicset = new ArrayList<>();
	private ArrayList<SingleSound> soundset = new ArrayList<>();
	private ArrayList<Animated> animset = new ArrayList<>();
	private ArrayList<Subtitle> subset = new ArrayList<>();
	
	//Лейтмотив, как лаконичная альтернатива коллекции из одного файла
	private Music leithmotive = null;
	
	//секундомер с точностью до 0.1 секунды
	private float playingTime;
	private long startTimeMillis;

	private boolean isPlaying = false;//почти как клавиша "Play"
	private TrueTypeFont font;
	private GameContainer c;
	
	/**
	 * Этот метод предназначен для инициализации катсцены при помощи XML-сценария
	 * */
	public void initWithScript(String filename, GameContainer c, TrueTypeFont font) {
		this.c = c;
		this.font = font;
		sourceScript = filename;
		if(sourceScript != null) parseScript(sourceScript, c, font);
	}
	
	private Color opaque_black = new Color(0, 0, 0, 0.7f);//цвет подложки субтитров
	
	/**Отрисовка катсцены на экране
	 * @param g инструменты отрисовки из инструментария Slick2D
	 * @param x координата отрисовки по-горизонтали
	 * @param y координата отрисовки по-вертикали
	 * */
	public void render(Graphics g, GameContainer c, float x, float y) {
		if(isPlaying) {
			if(font != null) g.setFont(font);
			
			//отрисовка кадра, в том случае, если секундомер проходит между его началом и концом 
			for(Frame frame: frameset) {
				if(playingTime > frame.start && playingTime < frame.end) g.drawImage(frame.image, x, y);
			}
			
			//отрисовка анимаций по секундомеру
			for(Animated animated: animset) {
				if(playingTime > animated.start && playingTime < animated.end) g.drawAnimation(animated.anim, x, y);
			}
			
			//отрисовка реплик по секундомеру
			for (Subtitle sub: subset) {
				Color init_color = g.getColor();
				
				if (playingTime > sub.start && playingTime < sub.end) {
					//черная подложка для читабельности
					g.setColor(opaque_black);
					g.fillRect(0, sub.y-5, c.getWidth(), (c.getHeight() - sub.y)+5);
					
					//текст
					Font font = g.getFont();
					int frame_width = c.getWidth();
					for (int i = 0; i < sub.lines.length; i++) {
						int width = font.getWidth(sub.lines[i]);
						g.setColor(Color.white);
						g.drawString(sub.lines[i], x+(frame_width - width)/2, y+sub.y+(font.getLineHeight()*i + 2));
					}
				}
				
				g.setColor(init_color);
			}
		}
	}
	
	/**
	 * Обновление катсцены
	 * */
	public void update(int delta) {
		if(isPlaying) {
			//проигрывание музыки
			for(Composition composition: musicset) {
				if(composition.end != -1) {
					if(playingTime > composition.start && !composition.played) {
						composition.music.play();
						composition.played = true;
					} 
					//если музыка играет, а не должна, выключаем её
					if(playingTime > composition.end && composition.music.playing()) {
						composition.music.stop();
					}
				}else {
					if(playingTime > composition.start && !composition.played){
						composition.music.play();
						composition.played = true;
					}
				}
			}
			
			//воспроизведение звука
			for (int i = 0; i < soundset.size(); i++) {
				SingleSound current_sound = soundset.get(i);
				//если звук ещё не играл - играем его
				if(playingTime > current_sound.start && !current_sound.played) {
					current_sound.sound.play();
					current_sound.played = true;//..и отключаем
				}
			}
			//таймер
			playingTime = Math.abs(startTimeMillis - System.currentTimeMillis())/1000f;
		}
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
	 * Добавление субтитров
	 * @param text текст субтитров
	 * @param start время начала отрисовки текста
	 * @param end время конца отрисовки (сек)
	 * */
	public void insertAt(String text, TrueTypeFont font, GameContainer c, float start, float end) {
		Subtitle sub = new Subtitle();
		sub.text = text;
		sub.start = start;
		sub.end = end;
		subset.add(sub);
		
		calculateSubtitleSizeAndPosition(font, c, sub);
	}
	
	/**
	 * Исключает все вхождения данного изображения из катсцены
	 * @param image изображение, которое следует исключить
	 * */
	public void remove(Image image) {
		for(Frame frame : frameset) 
			if(image.equals(frame.image)) frameset.remove(frame);
	}
	
	/**
	 * Исключает все вхождения этой композиции из катсцены
	 * @param music объект, который следует исключить
	 * */
	public void remove(Music music) {
		for(Composition soundtrack : musicset) 
			if(music.equals(soundtrack.music)) musicset.remove(soundtrack);
	}

	/**
	 * Исключает все вхождения звуковой дорожки из катсцены
	 * @param sound объект, который следует исключить
	 * */
	public void remove(Sound sound) {
		for(SingleSound soundtrack : soundset) 
			if(sound.equals(soundtrack.sound)) soundset.remove(soundtrack);
	}

	/**
	 * Исключает из катсцены все вхождения этой анимации 
	 * @param animation объект, который следует исключить
	 * */
	public void remove(Animation animation) {
		for(Animated animated: animset) 
			if(animation.equals(animated.anim)) animset.remove(animated);
	}	

	/**
	 * Исключает из катсцены все вхождения этих субтитров (только фразу целиком)
	 * @param text объект, который следует исключить
	 * */
	public void remove(String text) {
		for(Subtitle sub: subset) 
			if(text.equals(sub.text)) subset.remove(sub);
	}	
	
	/**
	 * Отступ от краёв окна в процентах от его ширины. 
	 * <br>Нормальное значение составляет 0.2f
	 * <br><i>Если значение больше 50% или меньше 0, то значение установится в 0.5 или 0 соответственно</i>
	 * */
	public void setLinewidthFactor(float linewidthFactor) {
		this.linewidthFactor = linewidthFactor < 0.5f ? linewidthFactor : 0.5f;
		this.linewidthFactor = linewidthFactor > 0 ? linewidthFactor : 0;
	}
	
	public float getLinewidthFactor() {
		return linewidthFactor;
	}
	
	/**Парсер XML-скрипта*/
	private boolean parseScript(String source, GameContainer c, TrueTypeFont font) {
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
			return false;
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
				Element element = (Element)music.item(i);
				String src = src_music + element.getAttribute("src");
				float start = Float.parseFloat(element.getAttribute("start"));
				if(element.hasAttribute("end")) {
					float end = Float.parseFloat(element.getAttribute("end"));
					insertAt(new Music(src), start, end);
				}else 
					insertAt(new Music(src), start);
			}

			//Парсинг звука
			NodeList sounds = document.getElementsByTagName("sound");
			for (int i = 0; i < sounds.getLength(); i++) {
				Element element = (Element)sounds.item(i);
				String src = src_sound + element.getAttribute("src");
				float start = Float.parseFloat(element.getAttribute("start"));
				insertAt(new Sound(src), start);
			}
			
			//Парсинг анимации
			NodeList animations = document.getElementsByTagName("animation");
			for (int i = 0; i < animations.getLength(); i++) {
				Element element = (Element)animations.item(i);
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
			
			//Парсинг субтитров
			NodeList subtitles = document.getElementsByTagName("line");
			for (int i = 0; i < subtitles.getLength(); i++) { 
				Element element = (Element)subtitles.item(i);
				String text = element.getTextContent();
				
				//начало и конец
				float start = Float.parseFloat(element.getAttribute("start"));
				float end = Float.parseFloat(element.getAttribute("end"));
				
				insertAt(text, font, c, start, end);
			}
			
			//Парсинг лейтмотива
			Element leithmotiveNode = (Element) document.getElementsByTagName("leithmotive").item(0);
			if(leithmotiveNode != null) {
				String src = src_music + leithmotiveNode.getAttribute("src");
				leithmotive = new Music(src);
			}
		} catch (SlickException e) {
			System.err.println("Ошибка парсинга! ");
			e.printStackTrace();
			return false;
		}
		return true;
	}	
	
	/**Запуск катсцены на воспроизведение.
	 * <br>Повторный вызов начнёт воспроизведение с начала ролика.
	 * */
	public void start() {		
		playingTime = 0;
		startTimeMillis = System.currentTimeMillis();
		if(leithmotive != null) {
			leithmotive.loop();
		}
		isPlaying = true;
	}
	
	/**Остановка воспроизведения*/
	public void stop() {
		isPlaying = false;
		if(leithmotive != null) leithmotive.stop();
		
		musicset.forEach((composition) -> {
			composition.music.stop();
		});
	}
	
	/**Возвращает время, прошедшее с момента старта катсцены*/
	public float playingAt() {
		return playingTime;
	}
	
	/**Вычисляет позицию на экране и размеры текста для текущего TrueType-шрифта*/
	private void calculateSubtitleSizeAndPosition(TrueTypeFont font, GameContainer c, Subtitle sub){
		boolean fits = false;//влезает или нет
		int lines_count = 1 + sub.text.length() - sub.text.replace("\n", "").length();
		
		float frame_width = c.getWidth();
		float frame_height = c.getHeight();
		float text_width = font.getWidth(sub.text);
		
		//если строка помещается в 0.8 размера экрана, то переносы не нужны
		if(frame_width - (frame_width*linewidthFactor) > text_width) {
			sub.width = text_width;
			fits = true;
		} else sub.width = frame_width - (frame_width*linewidthFactor);
		
		//если текст не поместился на одну строку, будут добавлены переносы
		if(!fits) {
			String[] words = sub.text.split(" ");
			StringBuilder newline = new StringBuilder();

			sub.text = "";
			for(int i = 0; i < words.length; i++) {	
				String line = newline.toString()+words[i];
				if(font.getWidth(line) < sub.width) {
					newline.append(words[i]+" ");
					sub.text += words[i]+" ";
				}
				else {
					sub.text += "\n"+words[i]+" ";
					newline = new StringBuilder(words[i]+" ");
					lines_count++;
				}
			}
			sub.lines = sub.text.split("\n");
		} else {
			sub.lines = new String[1];
			sub.lines[0] = sub.text;
		}
		
		//вычисление координат текста, в зависимости от его размеров
		sub.x = (frame_width - sub.width)/2;
		sub.y = (frame_height - 10) - font.getHeight(sub.text) * lines_count;
		sub.height = font.getHeight(sub.text) * lines_count;
	}
	
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
			parseScript(sourceScript, c, font);
		}
		
		//после перезагрузки, работавшая катсцена начнётся с начала
		if(played) start();
	}
	
	 /* TODO: Вынести парсинг в отдельный поток, чтобы избежать фризов
	  * upd: поток не содержащий GL-контекста не имеет права создавать GL-объекты, вопрос с потоком отложен на неопределённое время 
	  */
}
