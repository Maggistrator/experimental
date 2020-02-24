package com.catscene.demo;

import com.catscene.code.Cutsciene;
import java.awt.Font;

import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.BasicGame;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.SlickException;
import com.catscene.core.TrueTypeFont;

public class Start extends BasicGame{

	Cutsciene cs = new Cutsciene();    
	
	//�������, ������� ��������� ������ ������� �������� � �������������� ���������
	public static Font font = new Font("Courier New", Font.PLAIN, 16);
    public static TrueTypeFont slicFont;
	
    public Start(String title) {
		super(title);
		// TODO Auto-generated constructor stub
	}	
    
    @Override
	public void init(GameContainer container) throws SlickException { 
    	slicFont = new TrueTypeFont(font, true,("���������������������������������".toUpperCase()+"���������������������������������").toCharArray());
		cs.initWithScript("res/scripts/test.xml", container, slicFont);
		cs.start();
	}

	@Override
	public void render(GameContainer container, Graphics g) throws SlickException {
		g.setFont(slicFont);
		g.setColor(Color.white);
		cs.render(g, container, 0, 0);
		g.drawString(cs.playingAt()+"", container.getWidth()-40, 0);
	}

	@Override
	public void update(GameContainer container, int delta) throws SlickException {
		// TODO Auto-generated method stub
		cs.update(delta);
		if(cs.playingAt() > 15 && cs.playingAt() < 15.3f) cs.reload();
	}

	public static void main(String[] args) throws SlickException {
		AppGameContainer container = new AppGameContainer(new Start("cutsciene-test-app"));
		container.setTargetFrameRate(30);
		container.setDisplayMode(800, 600, false);
		container.start();
	}
	
}
