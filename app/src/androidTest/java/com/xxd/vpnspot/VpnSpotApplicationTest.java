package com.xxd.vpnspot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.TextView;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class VpnSpotApplicationTest {
    @Rule
    public ActivityScenarioRule<VpnSpotApplication> activityRule =
            new ActivityScenarioRule<>(VpnSpotApplication.class);

    @Test
    public void showsDefaultProxyInstructions() {
        activityRule.getScenario().onActivity(activity -> {
            EditText portInput = findEditText(activity.getWindow().getDecorView());
            assertNotNull(portInput);
            assertEquals("8080", portInput.getText().toString());

            TextView instructions = findTextViewContaining(
                    activity.getWindow().getDecorView(),
                    "How to connect:");
            assertNotNull(instructions);
            assertTrue(instructions.getText().toString().contains("port to 8080"));

            assertNotNull(findButton(activity.getWindow().getDecorView(), "Start"));
            assertNull(findButton(activity.getWindow().getDecorView(), "Stop"));
        });
    }

    @Test
    public void invalidPortShowsError() {
        activityRule.getScenario().onActivity(activity -> {
            EditText portInput = findEditText(activity.getWindow().getDecorView());
            assertNotNull(portInput);
            portInput.setText("70000");

            Button startButton = findButton(activity.getWindow().getDecorView(), "Start");
            assertNotNull(startButton);
            startButton.performClick();

            TextView status = findTextViewContaining(
                    activity.getWindow().getDecorView(),
                    "Invalid port. Use 1-65535.");
            assertNotNull(status);
        });
    }

    @Test
    public void logTabShowsEmptyState() {
        activityRule.getScenario().onActivity(activity -> {
            ProxyLogStore.clear();
            Button runTab = findButton(activity.getWindow().getDecorView(), "首页");
            Button logTab = findButton(activity.getWindow().getDecorView(), "数据");
            assertNotNull(runTab);
            assertNotNull(logTab);

            logTab.performClick();

            TextView emptyLog = findTextViewContaining(
                    activity.getWindow().getDecorView(),
                    "No connections yet.");
            assertNotNull(emptyLog);
        });
    }

    @Test
    public void logTabAllowsHorizontalScrollingForLongLines() {
        activityRule.getScenario().onActivity(activity -> {
            ProxyLogStore.clear();
            ProxyLogStore.addConnection(
                    "CONNECT",
                    "a-very-long-host-name-that-should-stay-on-one-scrollable-line.example.com",
                    443);

            View root = activity.getWindow().getDecorView();
            Button logTab = findButton(root, "数据");
            assertNotNull(logTab);
            logTab.performClick();

            HorizontalScrollView horizontalScroll = findHorizontalScrollViewContaining(
                    root,
                    "a-very-long-host-name-that-should-stay-on-one-scrollable-line.example.com:443");
            assertNotNull(horizontalScroll);
        });
    }

    @Test
    public void tabsFillBottomEdge() {
        activityRule.getScenario().onActivity(activity -> {
            View root = activity.getWindow().getDecorView();
            Button runTab = findButton(root, "首页");
            Button logTab = findButton(root, "数据");
            assertNotNull(runTab);
            assertNotNull(logTab);
            assertTrue(runTab.getCompoundDrawables()[1] != null);
            assertTrue(logTab.getCompoundDrawables()[1] != null);
            assertEquals(11f * activity.getResources().getDisplayMetrics().scaledDensity,
                    runTab.getTextSize(), 0.5f);
            assertEquals(11f * activity.getResources().getDisplayMetrics().scaledDensity,
                    logTab.getTextSize(), 0.5f);

            int expectedIconSize = (int) (28 * activity.getResources().getDisplayMetrics().density + 0.5f);
            assertTabIconSize(runTab, expectedIconSize);
            assertTabIconSize(logTab, expectedIconSize);

            assertEquals(root.getWidth(), runTab.getWidth() + logTab.getWidth());

            int[] runLocation = new int[2];
            int[] logLocation = new int[2];
            runTab.getLocationOnScreen(runLocation);
            logTab.getLocationOnScreen(logLocation);
            int tabBottom = Math.max(
                    runLocation[1] + runTab.getHeight(),
                    logLocation[1] + logTab.getHeight());
            int bottomTolerance = (int) (96 * activity.getResources().getDisplayMetrics().density);
            assertTrue(tabBottom >= root.getHeight() - bottomTolerance);
        });
    }

    @Test
    public void clearLogButtonClearsCurrentLog() {
        activityRule.getScenario().onActivity(activity -> {
            ProxyLogStore.clear();
            ProxyLogStore.addConnection("CONNECT", "www.baidu.com", 443);

            View root = activity.getWindow().getDecorView();
            Button logTab = findButton(root, "数据");
            assertNotNull(logTab);
            logTab.performClick();

            TextView baiduLog = findTextViewContaining(root, "CONNECT www.baidu.com:443");
            assertNotNull(baiduLog);

            Button clearButton = findButton(root, "Clear Log");
            assertNotNull(clearButton);
            clearButton.performClick();

            TextView emptyLog = findTextViewContaining(root, "No connections yet.");
            assertNotNull(emptyLog);
            assertTrue(ProxyLogStore.getEntries().isEmpty());
        });
    }

    @Test
    public void logTabRefreshesWhileVisible() throws Exception {
        activityRule.getScenario().onActivity(activity -> {
            ProxyLogStore.clear();
            View root = activity.getWindow().getDecorView();

            Button logTab = findButton(root, "数据");
            assertNotNull(logTab);
            logTab.performClick();

            TextView emptyLog = findTextViewContaining(root, "No connections yet.");
            assertNotNull(emptyLog);

            ProxyLogStore.addConnection("CONNECT", "realtime.example", 443);
        });

        Thread.sleep(1300);

        activityRule.getScenario().onActivity(activity -> {
            TextView refreshedLog = findTextViewContaining(
                    activity.getWindow().getDecorView(),
                    "CONNECT realtime.example:443");
            assertNotNull(refreshedLog);
        });
    }

    private TextView findTextViewContaining(View view, String text) {
        if (view instanceof TextView && ((TextView) view).getText().toString().contains(text)) {
            return (TextView) view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                TextView result = findTextViewContaining(group.getChildAt(i), text);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private EditText findEditText(View view) {
        if (view instanceof EditText) {
            return (EditText) view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                EditText result = findEditText(group.getChildAt(i));
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private Button findButton(View view, String text) {
        if (view instanceof Button && text.contentEquals(((Button) view).getText())) {
            return (Button) view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                Button result = findButton(group.getChildAt(i), text);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private HorizontalScrollView findHorizontalScrollViewContaining(View view, String text) {
        if (view instanceof HorizontalScrollView && findTextViewContaining(view, text) != null) {
            return (HorizontalScrollView) view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                HorizontalScrollView result = findHorizontalScrollViewContaining(group.getChildAt(i), text);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private void assertTabIconSize(Button button, int expectedSizePx) {
        Drawable icon = button.getCompoundDrawables()[1];
        assertNotNull(icon);
        assertEquals(expectedSizePx, icon.getBounds().width());
        assertEquals(expectedSizePx, icon.getBounds().height());
    }
}
