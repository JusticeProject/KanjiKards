package com.mozdeth.kanjikards;

import java.util.ArrayList;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;
//import android.util.Log;

public class WrappingTextView extends View
{
    private TextPaint m_topTextPaint = null;
    private TextPaint m_japaneseTextPaint = null;
    private TextPaint m_englishTextPaint = null;
    
    private Rect m_measuringRect = null;

    private String m_grade = "";
    private String m_deck = "";
    private String m_id = "";
    private String m_onReading = "";
    private String m_kunReading = "";
    private String m_english = "";

    private ArrayList<String> m_japaneseLines = null;
    private ArrayList<String> m_englishLines = null;

    private int m_padding = 0;
    private int m_heightOfMiddle = 0;
    
    //*************************************************************************
    
    public WrappingTextView(Context context)
    {
        super(context);
        //Log.v("MainActivity", "WrappingTextView constructor 1");
    }
    
    //*************************************************************************
    
    public WrappingTextView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        
        m_japaneseLines = new ArrayList<String>();
        m_englishLines = new ArrayList<String>();
        
        m_topTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        m_japaneseTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        m_englishTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        
        m_measuringRect = new Rect();
        
        m_topTextPaint.density = getResources().getDisplayMetrics().density;
        m_japaneseTextPaint.density = getResources().getDisplayMetrics().density;
        m_englishTextPaint.density = getResources().getDisplayMetrics().density;
        
        m_topTextPaint.setColor(Color.BLACK);
        m_japaneseTextPaint.setColor(Color.BLACK);
        m_englishTextPaint.setColor(Color.BLACK);
        
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.WrappingTextView, 0, 0);
        
        int mainCharSize = a.getDimensionPixelSize(R.styleable.WrappingTextView_mainCharSize, 45);
        m_japaneseTextPaint.setTextSize(mainCharSize);
        m_englishTextPaint.setTextSize(mainCharSize);
        
        int topCharSize = a.getDimensionPixelSize(R.styleable.WrappingTextView_topCharSize, 36);
        m_topTextPaint.setTextSize(topCharSize);
        
        m_padding = a.getDimensionPixelSize(R.styleable.WrappingTextView_padding, 3);
        //m_text = a.getString(R.styleable.WrappingTextView_defaultText); // why doesn't this work?
        a.recycle();
        
        //Log.v("MainActivity", "WrappingTextView constructor 2");
        //Log.v("MainActivity", "WrappingTextView dimTextSize=" + dimTextSize);
    }
    
    //*************************************************************************
    
    @Override
    public boolean isOpaque()
    {
        return true;
    }
    
    //*************************************************************************
    
    public void setText(String grade, String deck, String id, String onReading, String kunReading, String english)
    {
        m_grade = grade;
        m_deck = deck;
        m_id = id;
        m_onReading = cleanupString(onReading);
        m_kunReading = cleanupString(kunReading);
        m_english = cleanupString(english);
        
        invalidate();
        requestLayout();
    }
    
    //*************************************************************************
    
    private String cleanupString(String inputString)
    {
        String outputString = inputString;
        
        // only do the replace (which copies the String) if it is necessary
        if (outputString.contains("\n"))
        {
            outputString = outputString.replace("\n", " "); // text boxes have \n but no \r
        }
        
        if (outputString.contains("\t"))
        {
            outputString = outputString.replace("\t", " ");
        }
        
        // this is the unicode value for a Japanese space
        if (outputString.contains("\u3000"))
        {
            outputString = outputString.replace("\u3000", " ");
        }
        
        return outputString;
    }
    
    //*************************************************************************
    
    public void setTypeface(Typeface newTypeface)
    {
        m_japaneseTextPaint.setTypeface(newTypeface);
        invalidate();
        requestLayout();
    }

    //*************************************************************************
    
    @Override
    protected void onSizeChanged(int newWidth, int newHeight, int oldWidth, int oldHeight)
    {
        //Log.v("MainActivity", "onSizeChanged called with newWidth=" + newWidth + " newHeight=" + newHeight);
        
        // we are about to determine what words go on what lines, so start from scratch
        m_japaneseLines.clear();
        m_englishLines.clear();
        
        addWordsToLines(newWidth, m_onReading, m_japaneseTextPaint, m_japaneseLines);
        addWordsToLines(newWidth, m_kunReading, m_japaneseTextPaint, m_japaneseLines);
        int heightOfJapaneseLine = calculateHeightOfText(m_japaneseTextPaint);
        int heightOfJapaneseText = m_japaneseLines.size() * heightOfJapaneseLine + m_japaneseLines.size() * m_padding * 2;
        
        addWordsToLines(newWidth, m_english, m_englishTextPaint, m_englishLines);
        int heightOfEnglishLine = calculateHeightOfText(m_englishTextPaint);
        int heightOfEnglishText = m_englishLines.size() * heightOfEnglishLine + m_englishLines.size() * m_padding * 2;

        m_heightOfMiddle = heightOfJapaneseText + heightOfEnglishText;
        //Log.v("MainActivity", "heightOfMiddle=" + m_heightOfMiddle);
    }
    
    //*************************************************************************
    
    private void addWordsToLines(int width, String textToSplit, TextPaint paint, ArrayList<String> lines)
    {
        final int MAX_ALLOWED_WIDTH = width - (m_padding * 2);
        String[] words = textToSplit.split(" ");

        if (words == null || words.length == 0)
        {
            // if one has no lines, add an empty line
            lines.add("");
            return;
        }

        StringBuilder currentLine = new StringBuilder(textToSplit.length());
        currentLine.append(words[0]);

        for (int i = 1; i < words.length; i++)
        {
            if (words[i].length() == 0)
            {
                continue; // if the word is empty, skip it
            }
            
            if (currentLine == null)
            {
                currentLine = new StringBuilder(textToSplit.length());
            }

            if (currentLine.length() > 0)
            {
                currentLine.append(" "); // only add a space if there is already a word in there
            }
            
            currentLine.append(words[i]); // add the next word

            // alternate method: could also get the size of each individual word before entering this for loop
            paint.getTextBounds(currentLine.toString(), 0, currentLine.length(), m_measuringRect);

            // currently does not handle large words that need to wrap in the middle of the word
            if (m_measuringRect.width() > MAX_ALLOWED_WIDTH)
            {
                int startChar = currentLine.length() - words[i].length() - 1;
                
                // only delete if it is safe to do so, we're checking to see if the deletion will still leave chars left
                if (startChar > 0)
                {
                    // delete the last word that made it too long, we will add it to the next line shortly
                    currentLine.delete(startChar, currentLine.length());
                    
                    lines.add(currentLine.toString());
                    //Log.v("MainActivity", "determined line=" + currentLine + " of length=" + currentLine.length());
                    
                    currentLine = null;
                    currentLine = new StringBuilder(textToSplit.length());
                    
                    // add the deleted word to the next line
                    currentLine.append(words[i]);
                }
                else
                {
                    // it was not safe to delete the word, probably because it was the only word in the line, just use
                    // this as the line
                    lines.add(currentLine.toString());
                    //Log.v("MainActivity", "determined line=" + currentLine + " of length=" + currentLine.length());
                    
                    currentLine = null;
                    // don't allocate a new one because there may be no words left
                }
            }
        }

        if (currentLine != null)
        {
            lines.add(currentLine.toString());
            // Log.v("MainActivity", "determined line=" + currentLine + " of length=" + currentLine.length());
        }
        
        /* always test these scenarios:
         * h hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh blah blah
         *  hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh blah blah
         *  hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh
         * hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh
         *       hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh blah blah
         * blah blah hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh
         * blah blah hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh (with spaces at end)
         * blah hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh blah
         * 
         *  
         * blah blah blah blah blah blah blah blah blah blah blah blah
         * blah blah \n blah blah blah blah
         * hhhhhhhhhhhhhhhhhhhhhhhh \t hhhhhhhhhhhhhhhhhhhhhhh
         */

        return;
    }
    
    //*************************************************************************
    
    private int calculateHeightOfText(TextPaint paint)
    {
        String textToMeasure = "H";
        paint.getTextBounds(textToMeasure, 0, 1, m_measuringRect);
        int heightOfText = m_measuringRect.height();
        //Log.v("MainActivity", "heightOfText=" + heightOfText);
        return heightOfText;
    }
    
    //*************************************************************************
    
    private int calculateWidthOfGivenText(String textToMeasure, TextPaint paint)
    {
        if (textToMeasure.isEmpty())
        {
            return 0;
        }
        else
        {
            paint.getTextBounds(textToMeasure, 0, textToMeasure.length(), m_measuringRect);
            int widthOfGivenText = m_measuringRect.width();
            //Log.v("MainActivity", "widthOfGivenText=" + widthOfGivenText + " from text=" + textToMeasure);
            return widthOfGivenText;
        }
    }
    
    //*************************************************************************
    
    @Override
    protected void onDraw(Canvas canvas)
    {
        //Log.v("MainActivity", "onDraw");
        
        // draw the background
        canvas.drawColor(Color.WHITE);
        
        // draw the grade and deck in the upper left
        final int HEIGHT_OF_TOP_LINE = calculateHeightOfText(m_topTextPaint);
        canvas.drawText(m_grade, m_padding, m_padding + HEIGHT_OF_TOP_LINE, m_topTextPaint);
        canvas.drawText(m_deck, m_padding, 2*m_padding + 2*HEIGHT_OF_TOP_LINE, m_topTextPaint);
        
        // draw the id number in the upper right
        final int WIDTH_OF_ID = calculateWidthOfGivenText(m_id, m_topTextPaint);
        final int X_START_OF_ID = getRight() - m_padding - WIDTH_OF_ID;
        canvas.drawText(m_id, X_START_OF_ID, m_padding + HEIGHT_OF_TOP_LINE, m_topTextPaint);
        
        // get some data before drawing the middle lines
        final int WIDTH_OF_CANVAS = canvas.getWidth();
        final int HEIGHT_OF_JAPANESE_TEXT = calculateHeightOfText(m_japaneseTextPaint);
        final int HEIGHT_OF_ENGLISH_TEXT = calculateHeightOfText(m_englishTextPaint);
        int yStart = (getHeight() - m_heightOfMiddle) / 2;
        //Log.v("MainActivity", "width of canvas is " + WIDTH_OF_CANVAS);
        
        // draw the japanese lines in the middle, centered
        for (int i = 0; i < m_japaneseLines.size(); i++)
        {
            yStart += (m_padding + HEIGHT_OF_JAPANESE_TEXT);

            String line = m_japaneseLines.get(i);
            int widthOfText = calculateWidthOfGivenText(line, m_japaneseTextPaint);
            int xStart = (WIDTH_OF_CANVAS - widthOfText) / 2;
            
            //Log.v("MainActivity", "line " + line + " has width " + widthOfText + " and starts at " + xStart);
            
            canvas.drawText(line, xStart, yStart, m_japaneseTextPaint);
            
            yStart += m_padding;
        }

        // draw the english lines in the middle, centered
        for (int i = 0; i < m_englishLines.size(); i++)
        {
            yStart += (m_padding + HEIGHT_OF_ENGLISH_TEXT);

            String line = m_englishLines.get(i);
            int widthOfText = calculateWidthOfGivenText(line, m_englishTextPaint);
            int xStart = (WIDTH_OF_CANVAS - widthOfText) / 2;
            
            //Log.v("MainActivity", "line " + line + " has width " + widthOfText + " and starts at " + xStart);
            
            canvas.drawText(line, xStart, yStart, m_englishTextPaint);
            
            yStart += m_padding;
        }
    }
}
