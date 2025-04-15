package com.mozdeth.kanjikards;

import android.content.res.AssetManager;
import android.graphics.Typeface;

public class Utilities
{
	static Typeface m_font = null;
	static String[] m_deckNames = null;
	
	//*************************************************************************
	
	static void initJapaneseFont(AssetManager mng)
	{
		if (m_font == null)
		{
			m_font = Typeface.createFromAsset(mng, "fonts/DroidSansJapanese.ttf");
		}
	}
	
	//*************************************************************************
	
	static Typeface getJapaneseFont()
	{
		return m_font;
	}
	
	//*************************************************************************
	
	static void initDeckNames(String[] deckNames)
	{
		if (m_deckNames == null)
		{
			m_deckNames = deckNames;
		}
	}
	
	//*************************************************************************
	
	static String[] getDeckNames()
	{
		return m_deckNames;
	}
}
