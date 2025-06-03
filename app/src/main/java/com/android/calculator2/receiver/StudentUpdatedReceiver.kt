package com.android.calculator2.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.preference.PreferenceManager
import androidx.core.content.edit

class StudentUpdatedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.i(javaClass.getName(), "onReceive")

        // Customize the user interface to match the current Student's level
        val availableNumbers = intent.getStringArrayListExtra("availableNumbers")
        Log.i(javaClass.getName(), "availableNumbers: $availableNumbers")

        val availableNumeracySkills = intent.getStringArrayListExtra("availableNumeracySkills")
        Log.i(javaClass.getName(), "availableNumeracySkills: $availableNumeracySkills")

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        if (availableNumbers != null) {
            val availableNumberSet: MutableSet<String?> = HashSet<String?>()
            for (availableNumber in availableNumbers) {
                availableNumberSet.add(availableNumber)
            }
            Log.i(javaClass.getName(), "Storing availableNumbersSet: $availableNumberSet")
            sharedPreferences.edit(commit = true) {
                putStringSet(PREF_STUDENT_NUMBERS, availableNumberSet)
            }
        }

        if (availableNumeracySkills != null) {
            val availableNumeracySkillSet: MutableSet<String?> = HashSet<String?>()
            for (availableNumeracySkill in availableNumeracySkills) {
                availableNumeracySkillSet.add(availableNumeracySkill)
            }
            Log.i(
                javaClass.getName(),
                "Storing availableNumeracySkillSet: $availableNumeracySkillSet"
            )
            sharedPreferences.edit(commit = true) {
                putStringSet(PREF_STUDENT_NUMERACY_SKILLS, availableNumeracySkillSet)
            }
        }
    }

    companion object {
        const val PREF_STUDENT_NUMBERS: String = "pref_student_numbers"
        const val PREF_STUDENT_NUMERACY_SKILLS: String = "pref_student_numeracy_skills"
    }
}
