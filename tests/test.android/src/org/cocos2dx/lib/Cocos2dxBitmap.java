package org.cocos2dx.lib;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.Paint.Align;
import android.graphics.Paint.FontMetricsInt;

public class Cocos2dxBitmap{
	/*
	 * The values are the same as cocos2dx/platform/CCImage.h.
	 * I think three alignments are OK.
	 */
	private static final int ALIGNCENTER = 0x33;
	private static final int ALIGNLEFT	 = 0x31;
	private static final int ALIGNRIGHT	 = 0x32;
	
	/*
	 * @width: the width to draw, it can be 0
	 * @height: the height to draw, it can be 0
	 */
    public static void createTextBitmap(String content, String fontName, 
    		int fontSize, int alignment, int width, int height){
    	
    	// Avoid error when content is ""
    	if (content.compareTo("") == 0){
    		content = " ";
    	}
    	
    	Paint paint = newPaint(fontName, fontSize, alignment);
    	
    	TextProperty textProperty = getTextWidthAndHeight(content, paint, width, height);      	
      
        // Draw text to bitmap
        Bitmap bitmap = Bitmap.createBitmap(textProperty.maxWidth, 
        		textProperty.totalHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        
        // Draw string
        FontMetricsInt fm = paint.getFontMetricsInt();
        int x = 0;
        int y = -fm.ascent;
        String[] lines = textProperty.lines;
        for (String line : lines){
        	x = computeX(paint, line, textProperty.maxWidth, alignment);
        	canvas.drawText(line, x, y, paint);
        	y += textProperty.heightPerLine;
        }
        
        initNativeObject(bitmap);
    }
    
    private static int computeX(Paint paint, String content, int w, int alignment){
    	int ret = 0;
    	
    	switch (alignment){
    	case ALIGNCENTER:
    		ret = w / 2;
    		break;
    	
        // ret = 0
    	case ALIGNLEFT:   		
    		break;
    		
    	case ALIGNRIGHT:
    		ret = w;
    		break;
    	
    	/*
    	 * Default is align left.
    	 * Should be same as newPaint().
    	 */
    	default:
    		break;
    	}
    	
    	return ret;
    }
    
    private static class TextProperty{
    	// The max width of lines
    	int maxWidth;
    	// The height of all lines
    	int totalHeight;
    	int heightPerLine;
    	String[] lines;

    	TextProperty(int w, int h, String[] lines){
    		this.maxWidth = w;
    		this.heightPerLine = h;
    		this.totalHeight = h * lines.length;   		
    		this.lines = lines;
    	}
    }
    
    private static TextProperty getTextWidthAndHeight(String content, Paint paint,
    		int maxWidth, int maxHeight){              
        FontMetricsInt fm = paint.getFontMetricsInt();
        int h = (int)Math.ceil(fm.descent - fm.ascent);
        int maxContentWidth = 0;
        
        String[] lines = splitString(content, maxHeight, maxWidth, paint);
        
        if (maxWidth != 0){
        	maxContentWidth = maxWidth;
        }
        else {
        	/*
             * Compute the max width
             */
            int temp = 0;
            for (String line : lines){
            	temp = (int)Math.ceil(paint.measureText(line, 0, line.length()));
            	if (temp > maxContentWidth){
            		maxContentWidth = temp;
            	}
            }
        }
        
        
        return new TextProperty(maxContentWidth, h, lines);
    }
    
    /*
     * If maxWidth or maxHeight is not 0,
     * split the string to fix the maxWidth and maxHeight.
     */
    private static String[] splitString(String content, int maxHeight, int maxWidth, 
    		Paint paint){
    	String[] lines = content.split("\\n");
    	String[] ret = null;
    	FontMetricsInt fm = paint.getFontMetricsInt();
        int heightPerLine = (int)Math.ceil(fm.descent - fm.ascent);
        int maxLines = maxHeight / heightPerLine;
    	
    	if (maxWidth != 0){
    		LinkedList<String> strList = new LinkedList<String>();
    		for (String line : lines){
    			/*
    			 * The width of line is exceed maxWidth, should divide it into
    			 * two or more lines.
    			 */
    			int lineWidth = (int)Math.ceil(paint.measureText(line));
    			if (lineWidth > maxWidth){    				
    				strList.addAll(divideStringWithMaxWidth(paint, line, maxWidth));
    			}
    			else{
    				strList.add(line);
    			}
    			
    			/*
    			 * Should not exceed the max height;
    			 */
    			if (maxLines > 0 && strList.size() >= maxLines){
    				break;
    			}
    		}
    		
    		/*
    		 * Remove exceeding lines
    		 */
    		if (maxLines > 0 && strList.size() > maxLines){
    			while (strList.size() > maxLines){
    				strList.removeLast();
    			}
    		}
    		
    		ret = new String[strList.size()];
    		strList.toArray(ret);
    	} else 
    	if (maxHeight != 0 && lines.length > maxLines) {
    		/*
    		 * Remove exceeding lines
    		 */
    		LinkedList<String> strList = new LinkedList<String>();
    		for (int i = 0; i < maxLines; i++){
    			strList.add(lines[i]);
    		}
    		ret = new String[strList.size()];
    		strList.toArray(ret);
    	}
    	else {
    		ret = lines;
    	}
    	
    	return ret;
    }
    
    private static LinkedList<String> divideStringWithMaxWidth(Paint paint, String content, 
    		int width){
    	int charLength = content.length();
    	int start = 0;
    	int tempWidth = 0;
    	LinkedList<String> strList = new LinkedList<String>();
    	
    	/*
    	 * Break a String into String[] by the width & should wrap the word
    	 */
    	for (int i = 1; i <= charLength; ++i){  		
    		tempWidth = (int)Math.ceil(paint.measureText(content, start, i));
    		if (tempWidth >= width){
    			int lastIndexOfSpace = content.substring(0, i).lastIndexOf(" ");
    			
    			if (lastIndexOfSpace != -1){
    				/**
    				 * Should wrap the word
    				 */
    				strList.add(content.substring(start, lastIndexOfSpace));
    				i = lastIndexOfSpace;
    			}
    			else {
    				/*
        			 * Should not exceed the width
        			 */
        			if (tempWidth > width){
        				strList.add(content.substring(start, i - 1));
        				/*
        				 * compute from previous char
        				 */
        				--i;
        			}
        			else {
        				strList.add(content.substring(start, i));   				
        			}
    			}
    			   			
    			start = i;
    		}
    	}
    	
    	/*
    	 * Add the last char
    	 */
    	if (start == charLength - 1){
    		strList.add(content.substring(charLength-1));
    	}
    	
    	return strList;
    }
    
    private static Paint newPaint(String fontName, int fontSize, int alignment){
    	Paint paint = new Paint();
    	paint.setColor(Color.WHITE);
        paint.setTextSize(fontSize);
        paint.setTypeface(Typeface.create(fontName, Typeface.NORMAL));
        paint.setAntiAlias(true);
        
        switch (alignment){
    	case ALIGNCENTER:
    		paint.setTextAlign(Align.CENTER);
    		break;
    	
    	case ALIGNLEFT:  
    		paint.setTextAlign(Align.LEFT);
    		break;
    		
    	case ALIGNRIGHT:
    		paint.setTextAlign(Align.RIGHT);
    		break;
    	
    	default:
    		paint.setTextAlign(Align.LEFT);
    		break;
    	}
        
        return paint;
    }
    
    private static void initNativeObject(Bitmap bitmap){
    	byte[] pixels = getPixels(bitmap);
    	if (pixels == null){
    		return;
    	}
    	
    	nativeInitBitmapDC(bitmap.getWidth(), bitmap.getHeight(), pixels);
    }
    
    private static byte[] getPixels(Bitmap bitmap){
    	if (bitmap != null){
    		byte[] pixels = new byte[bitmap.getWidth() * bitmap.getHeight() * 4];
    		ByteBuffer buf = ByteBuffer.wrap(pixels);
    		buf.order(ByteOrder.nativeOrder());
    		bitmap.copyPixelsToBuffer(buf);
    		return pixels;
    	}
    	
    	return null;
    }
    
    private static native void nativeInitBitmapDC(int width, int height, byte[] pixels);
}
