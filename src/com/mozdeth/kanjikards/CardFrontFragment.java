package com.mozdeth.kanjikards;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mozdeth.kanjikards.database.KanjiEntry;


public class CardFrontFragment extends Fragment
{
    private static final String KANJI_KEY = "CARD_KANJI";
    private String m_kanji = "";
    
    //*************************************************************************
    
    public static CardFrontFragment newInstance(KanjiEntry entry)
    {
        CardFrontFragment cardFragment = new CardFrontFragment();
        
        if (entry != null)
        {
            // send the arguments to the fragment that we created
            Bundle args = new Bundle();
            args.putString(KANJI_KEY, entry.getKanji());
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
                m_kanji = args.getString(KANJI_KEY);
            }
        }
        else
        {
            m_kanji = savedInstanceState.getString(KANJI_KEY);
        }
    }
    
    //*************************************************************************

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_card_front, container, false);

        TextView kanjiText = (TextView)rootView.findViewById(R.id.textKanji);
        kanjiText.setText(m_kanji);
        kanjiText.setTypeface(Utilities.getJapaneseFont());
        
        return rootView;
    }
    
    //*************************************************************************
    
    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putString(KANJI_KEY, m_kanji);
    }
}