package com.mozdeth.kanjikards.database;

import java.util.Locale;

public class KanjiEntry
{
	public static KanjiEntry blankKanji = new KanjiEntry(0, "!", "", "", "No cards in current deck.\nSwipe down to load a different deck.", -1);
	
	private int m_id;
	private String m_kanji;
	private String m_on;
	private String m_kun;
	private String m_english;
	private int m_deck;
	private String m_grade;
	
	private String m_onNoParentheses;
	private String m_kunNoParentheses;
	private String m_englishLowerCase;
	
    //*********************************************************************************************
	
	public KanjiEntry(int id, String kanji, String on, String kun, String english, int deck)
	{
		this.m_id = id;
		this.m_kanji = kanji;
		setOn(on); // the set functions will handle creating the searchable versions
		setKun(kun);
		setEnglish(english);
		this.m_deck = deck;

		if (m_id > 1006)
		{
			m_grade = "Grade 7";
		}
		else if (m_id > 825)
		{
			m_grade = "Grade 6";
		}
		else if (m_id > 640)
		{
			m_grade = "Grade 5";
		}
		else if (m_id > 440)
		{
			m_grade = "Grade 4";
		}
		else if (m_id > 240)
		{
			m_grade = "Grade 3";
		}
		else if (m_id > 80)
		{
			m_grade = "Grade 2";
		}
		else if (m_id > 0)
		{
			m_grade = "Grade 1";
		}
		else
		{
			m_grade = "";
		}
	}
	
    //*********************************************************************************************
	
	public int getId()
	{
		return m_id;
	}
	
    //*********************************************************************************************
	
	public String getKanji()
	{
		return m_kanji;
	}
	
    //*********************************************************************************************
	
	public String getOn()
	{
		return m_on;
	}
	
    //*********************************************************************************************
	
	public void setOn(String newOn)
	{
		// set the normal on reading
		m_on = newOn;

		// set the searchable on reading
		m_onNoParentheses = newOn.replace("(", "").replace(")", "");
	}
	
    //*********************************************************************************************
	
	public String getKun()
	{
		return m_kun;
	}
	
    //*********************************************************************************************
	
	public void setKun(String newKun)
	{
		// set the normal kun reading
		m_kun = newKun;

		// set the searchable kun reading
		m_kunNoParentheses = newKun.replace("(", "").replace(")", "");
	}
	
    //*********************************************************************************************
	
	public String getEnglish()
	{
		return m_english;
	}
	
    //*********************************************************************************************
	
	public void setEnglish(String newEnglish)
	{
		// set the normal english translation
		m_english = newEnglish;
		
		// set the searchable english translation
		m_englishLowerCase = newEnglish.toLowerCase(Locale.US);
	}
	
    //*********************************************************************************************
	
	public int getDeck()
	{
		return m_deck;
	}
	
    //*********************************************************************************************
	
	public void setDeck(int requestedDeck)
	{
		this.m_deck = requestedDeck;
	}
	
    //*********************************************************************************************
	
	public String getGrade()
	{
		return m_grade;
	}
	
	//*********************************************************************************************
	
	public boolean containsQuery(String noParenthesesQuery, String lowerCaseQuery)
	{
		if (m_kanji.equals(noParenthesesQuery))
		{
			return true;
		}
		
		if (m_onNoParentheses.contains(noParenthesesQuery))
		{
			return true;
		}
		
		if (m_kunNoParentheses.contains(noParenthesesQuery))
		{
			return true;
		}
		
		if (m_englishLowerCase.contains(lowerCaseQuery))
		{
			return true;
		}
		
		// if we get this far then we haven't found it yet
		return false;
	}
}
