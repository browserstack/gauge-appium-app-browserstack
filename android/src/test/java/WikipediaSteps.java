import com.thoughtworks.gauge.AfterSpec;
import com.thoughtworks.gauge.BeforeSpec;
import com.thoughtworks.gauge.Step;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Gauge step implementations for the BrowserStack App Automate (Appium, Android) sample.
 *
 * The driver is created with an EMPTY options object against the BrowserStack hub.
 * The BrowserStack Java SDK (attached as a -javaagent to the Gauge runner JVM) injects the
 * app + device capabilities from browserstack.yml at session creation time.
 */
public class WikipediaSteps {

    private static final String HUB_URL = "https://hub.browserstack.com/wd/hub";
    private static WebDriver driver;

    @BeforeSpec
    public void setUp() throws Exception {
        // Empty options — the BrowserStack SDK injects app/device caps from browserstack.yml.
        UiAutomator2Options options = new UiAutomator2Options();
        driver = new AndroidDriver(new URL(HUB_URL), options);
    }

    // ===== Sample test: WikipediaSample.apk =====

    @Step("I tap on the Search Wikipedia button")
    public void tapSearchWikipedia() {
        WebElement search = new WebDriverWait(driver, Duration.ofSeconds(30)).until(
                ExpectedConditions.elementToBeClickable(AppiumBy.accessibilityId("Search Wikipedia")));
        search.click();
    }

    @Step("I search with keyword <keyword>")
    public void searchWithKeyword(String keyword) {
        WebElement input = new WebDriverWait(driver, Duration.ofSeconds(30)).until(
                ExpectedConditions.elementToBeClickable(
                        AppiumBy.id("org.wikipedia.alpha:id/search_src_text")));
        input.sendKeys(keyword);
    }

    @Step("The search results should be listed")
    public void searchResultsShouldBeListed() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    AppiumBy.className("android.widget.TextView")));
        } catch (TimeoutException e) {
            throw new AssertionError("Search results did not load within 30 seconds");
        }
        List<WebElement> results = driver.findElements(AppiumBy.className("android.widget.TextView"));
        assertThat(results)
                .withFailMessage("Expected the Wikipedia search to list results, but none were found")
                .isNotEmpty();
    }

    // ===== Local test: LocalSample.apk =====

    @Step("I am on the local app")
    public void iAmOnTheLocalApp() throws InterruptedException {
        WebElement action = new WebDriverWait(driver, Duration.ofSeconds(30)).until(
                ExpectedConditions.elementToBeClickable(
                        AppiumBy.id("com.example.android.basicnetworking:id/test_action")));
        action.click();
        Thread.sleep(5000);
    }

    @Step("I verify active connection on the app")
    public void iVerifyActiveConnection() {
        new WebDriverWait(driver, Duration.ofSeconds(30)).until(
                ExpectedConditions.presenceOfElementLocated(
                        AppiumBy.className("android.widget.TextView")));
        List<WebElement> textViews = driver.findElements(AppiumBy.className("android.widget.TextView"));
        WebElement matched = null;
        for (WebElement el : textViews) {
            if (el.getText() != null && el.getText().contains("The active connection is")) {
                matched = el;
                break;
            }
        }
        if (matched == null) {
            throw new AssertionError(
                    "Could not find any TextView containing 'The active connection is' — "
                            + "is the BrowserStack Local tunnel connected?");
        }
        String text = matched.getText();
        System.out.println("Local app reported: " + text);
        assertThat(text)
                .withFailMessage("Expected the local app to report it is up and running, got: %s", text)
                .contains("Up and running");
    }

    @AfterSpec
    public void tearDown() {
        if (driver != null) {
            driver.quit();
            driver = null;
        }
    }
}
