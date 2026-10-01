package com.seekerspark.game

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeviceSmokeTest {
    @Test fun launchStartAndExitRound() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withText("Seeker Spark")).check(matches(isDisplayed()))
            onView(withText("Connect Seeker wallet")).check(matches(isDisplayed()))
            onView(withText("Play 30-second round")).perform(click())
            onView(withText("End round")).check(matches(isDisplayed())).perform(click())
            onView(withText("End this round?")).check(matches(isDisplayed()))
            onView(withId(android.R.id.button1)).perform(click())
            onView(withText("Play 30-second round")).check(matches(isDisplayed()))
        }
    }
}
