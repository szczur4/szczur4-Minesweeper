package szczur4;
import java.awt.image.BufferedImage;
public class Util{
	public static BufferedImage maskImage(BufferedImage img,int mask){
		BufferedImage newImg=new BufferedImage(img.getWidth(),img.getHeight(),BufferedImage.TYPE_INT_ARGB);
		newImg.createGraphics().drawImage(img,0,0,null);
		for(int x=0;x<img.getWidth();x++)for(int y=0;y<img.getHeight();y++)newImg.setRGB(x,y,newImg.getRGB(x,y)&mask);
		return newImg;
	}
	public static BufferedImage rotateImg(BufferedImage img){
		int w=img.getWidth(),h=img.getHeight();
		BufferedImage rotated=new BufferedImage(h,w,BufferedImage.TYPE_INT_ARGB);
		for(int x=0;x<w;x++)for(int y=0;y<h;y++)rotated.setRGB(h-1-y,x,img.getRGB(x,y));
		return rotated;
	}
}
