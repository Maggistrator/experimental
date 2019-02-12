package code;

import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.BasicGame;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.SlickException;

public class Start extends BasicGame{

	Cutsciene cs;
	public Start(String title) {
		super(title);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void render(GameContainer container, Graphics g) throws SlickException {
		// TODO Auto-generated method stub
		cs.render(g, 0, 0);
		g.drawString(cs.playingAt()+"", container.getWidth()-40, 0);
	}

	@Override
	public void init(GameContainer container) throws SlickException {
		cs = new Cutsciene("res/scripts/test.xml");
		cs.start();
	}

	@Override
	public void update(GameContainer container, int delta) throws SlickException {
		// TODO Auto-generated method stub
		cs.update(delta);
	}

	public static void main(String[] args) throws SlickException {
		AppGameContainer container = new AppGameContainer(new Start("pidor-test-app"));
		container.setTargetFrameRate(30);
		container.start();
	}
	
}
