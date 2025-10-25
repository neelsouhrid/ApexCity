package com.example.apexcity.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.apexcity.R

class LeaderboardFragment : Fragment() {
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_leaderboard, container, false)
        
        // TODO: Implement leaderboard functionality
        view.findViewById<TextView>(R.id.tvComingSoon)?.text = "Leaderboard Coming Soon!"
        
        return view
    }
}