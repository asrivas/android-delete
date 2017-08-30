package drive.play.android.samples.com.drivedeletesample;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class MainActivityEspressoTest {
  @Rule
  public ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule<>(MainActivity.class);

  @Test
  public void displayUI_mainActivity() {
    onView(withId(R.id.submitButton)).check(matches(isDisplayed()));
    onView(withId(R.id.resetButton)).check(matches(isDisplayed()));
    onView(withId(R.id.questionTextView)).check(matches(isDisplayed()));
    onView(withId(R.id.answerEditText)).check(matches(isDisplayed()));
  }
}
