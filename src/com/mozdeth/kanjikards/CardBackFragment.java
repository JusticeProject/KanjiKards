package com.mozdeth.kanjikards;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mozdeth.kanjikards.database.KanjiEntry;


public class CardBackFragment extends Fragment
{
    private static final String GRADE_KEY = "CARD_KANJI";
    private static final String DECK_KEY = "CARD_DECK";
    private static final String ID_KEY = "CARD_ID";
    private static final String ON_KEY = "ON_READING";
    private static final String KUN_KEY = "KUN_READING";
    private static final String ENGLISH_KEY = "ENGLISH_READING";
    private String m_grade = "";
    private String m_deck = "";
    private String m_id = "";
    private String m_onReading = "";
    private String m_kunReading = "";
    private String m_english = "";
    
    //*************************************************************************
    
    public static CardBackFragment newInstance(KanjiEntry entry)
    {
        CardBackFragment cardFragment = new CardBackFragment();
        
        // get the name of the deck
        String[] deckNames = Utilities.getDeckNames();
        String deck = "";
        if (entry.getDeck() >= 0)
        {
            deck = deckNames[entry.getDeck()];
        }
        
        if (entry != null)
        {
            Bundle args = new Bundle();
            args.putString(GRADE_KEY, entry.getGrade());
            args.putString(DECK_KEY, deck);
            args.putString(ID_KEY, String.valueOf(entry.getId()));
            args.putString(ON_KEY, entry.getOn());
            args.putString(KUN_KEY, entry.getKun());
            args.putString(ENGLISH_KEY, entry.getEnglish());
            cardFragment.setArguments(args);
        }
        
        return cardFragment;
    }
    
    //*************************************************************************
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        if (savedInstanceState == null)
        {
            Bundle args = getArguments();
            if (args != null)
            {
                m_grade = args.getString(GRADE_KEY);
                m_deck = args.getString(DECK_KEY);
                m_id = args.getString(ID_KEY);
                m_onReading = args.getString(ON_KEY);
                m_kunReading = args.getString(KUN_KEY);
                m_english = args.getString(ENGLISH_KEY);
            }
        }
        else
        {
            m_grade = savedInstanceState.getString(GRADE_KEY);
            m_deck = savedInstanceState.getString(DECK_KEY);
            m_id = savedInstanceState.getString(ID_KEY);
            m_onReading = savedInstanceState.getString(ON_KEY);
            m_kunReading = savedInstanceState.getString(KUN_KEY);
            m_english = savedInstanceState.getString(ENGLISH_KEY);
        }
    }
    
    //*************************************************************************

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_card_back, container, false);
        
        // just in case something goes wrong during the upgrade, this can be removed in later versions
        if (m_deck == null)
        {
            m_deck = "";
        }

        // set the text
        WrappingTextView text = (WrappingTextView)rootView.findViewById(R.id.textWrapper);
        text.setTypeface(Utilities.getJapaneseFont());
        text.setText(m_grade, m_deck, m_id, m_onReading, m_kunReading, m_english);

        return rootView;
    }
    
    //*************************************************************************
    
    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        
        outState.putString(GRADE_KEY, m_grade);
        outState.putString(DECK_KEY, m_deck);
        outState.putString(ID_KEY, m_id);
        outState.putString(ON_KEY, m_onReading);
        outState.putString(KUN_KEY, m_kunReading);
        outState.putString(ENGLISH_KEY, m_english);
    }
}