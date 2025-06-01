package org.prac.korailreserve;

import io.github.bonigarcia.wdm.WebDriverManager;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.Locale;
import java.util.NoSuchElementException;

import static java.lang.Thread.sleep;

@Service
public class TicketService {

    @Autowired
    private EmailService emailService;

    @Value("${app.email.recipient}") // Matches the path in application.yml
    private String defaultRecipientEmail; // Or just 'recipientEmail' if you prefer


    public String reserveTicket(String txtMember, String txtPwd, String txtGoStart, String txtGoEnd, String selMonth,
                                String selDay, Integer startHour, Integer startMin, Integer endHour, Integer endMin) {
        WebDriver driver = null;
        StringBuilder result = new StringBuilder();

        LocalTime startTime = LocalTime.of(startHour, startMin);
        // extract hour in 24-hour format
        String startHourIn24Format = startTime.getHour() < 10 ? "0" + startTime.getHour() : String.valueOf(startTime.getHour());
        LocalTime endTime = LocalTime.of(endHour, endMin);

        try {
            driver = initializeWebDriver();
            loginUser(driver, txtMember, txtPwd);
            navigateToReservationPage(driver,txtGoStart, txtGoEnd, selMonth, selDay, startHourIn24Format, startTime,
                    endTime);
            // Check and reserve ticket

            boolean ticketReserved = checkAndReserveTicket(driver, txtGoStart, txtGoEnd, selMonth, selDay, startHour, startTime,
                    endTime);
//
//            if (ticketReserved) {
//                result.append("Ticket Reserved !");
//            } else {
//                result.append("No more pages. Ticket not found.");
//            }
        } catch (Exception e) {
            result.append("Error fetching data: ").append(e.getMessage());
        }
        return result.toString();
    }

    // Web driver initialize
    private WebDriver initializeWebDriver() {
        // Chrome Driver
        WebDriverManager.chromedriver().setup();

        // Chrome option : headless
        ChromeOptions options = new ChromeOptions();
//        options.addArguments("--headless");

        WebDriver driver = new ChromeDriver(options);

        // Safari Drvier
        // System.setProperty("webdriver.safari.driver", "/System/Cryptexes/App/usr/bin/safaridriver");
        // WebDriver driver = new SafariDriver();
        driver.manage().window().setSize(new Dimension(1600, 1200));
        return driver;
    }
    private void close_window(WebDriver driver) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofMillis(500));
        try {
            WebElement closeButton = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("button.btn_by-blue.btn_pop-close")
            ));
            closeButton.click();
        } catch (TimeoutException e) {
            System.out.println("창닫기 버튼이 나타나지 않았습니다. 건너뜁니다.");
        }
    }
    private void close_ok_window(WebDriver driver) {
        // Close the popup window if it appears
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofMillis(500));
        try {
            WebElement closeButton = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("button.btn_bn-blue.btn_pop-close")
            ));
            closeButton.click();
        } catch (TimeoutException e) {
            System.out.println("확인 버튼이 나타나지 않았습니다. 건너뜁니다.");
        }
    }
    // Login process
    private void loginUser(WebDriver driver, String txtMember, String txtPwd) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

        driver.get("https://www.korail.com/ticket/login");

        close_window(driver);

        // Input 회원번호
        WebElement memberInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("id")));
        memberInput.clear();
        memberInput.sendKeys(txtMember);

        // Input 비밀번호
        WebElement passwordInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("password")));
        passwordInput.clear();
        passwordInput.sendKeys(txtPwd);

        // Step 1: Wait for and find the div with id="tab_memNum"
        WebElement tabMemNum = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("tab_memNum")));

// Step 2: Inside that div, find the 로그인 button with class="btn_bn-depblue"
        WebElement loginButton = tabMemNum.findElement(By.cssSelector("button.btn_bn-depblue"));

// Step 3: Click the button
        loginButton.click();


        wait.until(ExpectedConditions.urlContains("https://www.korail.com/ticket/main"));

    }
    public void selectHour(WebDriver driver, WebDriverWait wait, String hourText) throws InterruptedException {

        // XPath for visible (active) slides
        String hourXPath = "//a[normalize-space(text())='" + hourText + "']";

        int maxTries = 5;
        int tries = 0;

        while (tries < maxTries) {
            try {
                // Try to find the hour element in visible slides
                WebElement hourElement = driver.findElement(By.xpath(hourXPath));
                if (hourElement.isDisplayed() && hourElement.isEnabled()) {
                    hourElement = wait.until(ExpectedConditions.elementToBeClickable(
                            By.xpath("//a[normalize-space(text())='" + hourText + "']")
                    ));
                    hourElement.click();
                    System.out.println("Clicked hour: " + hourText);
                    return;
                }
            } catch (NoSuchElementException | TimeoutException e) {
                // Element not found or not clickable, continue scrolling
            }

            // Click next button to scroll
            // Scroll with "Next" button
            try {
                // Scroll using the Next button inside timeSelect
                WebElement nextButton = driver.findElement(By.xpath("//div[@class='timeSelect']//button[contains(@class, 'slick-next') and @aria-disabled='false']"));
                wait.until(ExpectedConditions.elementToBeClickable(nextButton)).click();
                Thread.sleep(1000); // let it animate
            } catch (Exception e) {
                System.out.println("❌ Cannot click Next button: " + e.getMessage());
                break;
            }
            tries++;
        }

        throw new RuntimeException("Hour '" + hourText + "' not found in slick slider after " + maxTries + " tries.");
    }
    // To reserve Page
    private void navigateToReservationPage(WebDriver driver, String txtGoStart, String txtGoEnd, String selMonth,
                                           String selDay, String startHour, LocalTime startTime, LocalTime endTime) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        close_window(driver);
        // 2. Click 출발역 선택 버튼
        WebElement btnStart = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".btn_start")));
        btnStart.click();
        // Now select txtGoStart in the popup (implementation depends on popup structure)
        // Use XPath to find the <a> tag containing the exact text
        WebElement startStation = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//span[@class='ch_tag']/a[text()='" + txtGoStart + "']")
        ));

// Click the element
        startStation.click();


        // 3. Click 도착역 선택 버튼
        WebElement btnEnd = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".btn_end")));
        btnEnd.click();
        // Now select txtGoEnd in the popup
        // Use XPath to find the <a> tag containing the exact text
        WebElement endStation = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//span[@class='ch_tag']/a[text()='" + txtGoEnd + "']")
        ));

// Click the element
        endStation.click();

        // 4. Click 출발일 선택 버튼
        WebElement btnDate = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".btn_brth")));
        btnDate.click();
        // Set formattedDate using UI in popup
        // 달력 팝업 뜸

        // 1. Parse selMonth (e.g., "06") and get current month
        int selectedMonth = Integer.parseInt(selMonth); // example: "06"
        int currentMonth = LocalDate.now().getMonthValue(); // from java.time

        // 2. If selected month is later than current, click the slick-next button ( 최대 다음달)
        if (selectedMonth > currentMonth) {
            WebElement nextButton = driver.findElement(By.cssSelector("button.slick-next"));
            nextButton.click();
        }
        // XPath to locate the <a> tag inside a <td> containing <span class="day">31</span>
        WebElement dayElement = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//td[a/span[@class='day' and text()='" + selDay + "']]//a")
        ));

// Click it
        // wait until dayElement clicked
        dayElement.click();
        System.out.println(startHour);
        // wait until date is selected
        try {
            sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        String hourText = startHour + "시";
        // XPath to find the <a> tag that contains the text "23시"
        System.out.println(hourText);
//        WebElement hourElement = wait.until(ExpectedConditions.elementToBeClickable(
//                By.xpath("//div[contains(@class,'slick-active')]//a[normalize-space(text())='" + hourText + "']")
//        ));
        //        hourElement.click();
        // use select hour method
        try {
            selectHour(driver, wait, hourText);
        } catch (Exception e) {
            System.out.println("Error selecting hour: " + e.getMessage());
            throw new RuntimeException("Failed to select hour: " + hourText);
        }
// Click it


        WebElement applyButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[normalize-space(text())='적용']")
        ));
        applyButton.click();

        // 5. Click "열차 조회하기"
        WebElement searchTrainButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[normalize-space(text())='열차 조회하기']")
        ));
        searchTrainButton.click();

        // 6. Wait for the results to load
        // wait until https://www.korail.com/ticket/search/list page
        wait.until(ExpectedConditions.urlContains("https://www.korail.com/ticket/search/list"));
    }

    // Ticket Reserve process
    private boolean checkAndReserveTicket(WebDriver driver, String txtGoStart, String txtGoEnd, String selMonth,
                                          String selDay, Integer startHour, LocalTime startTime, LocalTime endTime) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

        close_window(driver);

        boolean isTicketFound = false;

//        setTicketSearchCriteria(driver, wait, txtGoStart, txtGoEnd, startHour, selMonth, selDay);
//        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".btn_inq a"))).click();
        //refresh
        isTicketFound = findAndReserveAvailableTicket(driver, startTime, endTime);
        driver.navigate().refresh();
        wait.until(ExpectedConditions.urlContains("https://www.korail.com/ticket/search/list"));
        close_window(driver);

        while (!isTicketFound) {
//            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("tableResult")));

            isTicketFound = findAndReserveAvailableTicket(driver, startTime, endTime);

            if (!isTicketFound) {
                try {
                    sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
//                wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".btn_inq a"))).click();

                driver.navigate().refresh();
                wait.until(ExpectedConditions.urlContains("https://www.korail.com/ticket/search/list"));
                close_window(driver);
            } else {
                isTicketFound = true;
                // Send email when the task is finished

                String subject = "코레일 기차 예약 완료";
                String body = "코레일 기차 예약이 완료되었습니다.\n\n" +
                        "출발역: " + txtGoStart + "\n" +
                        "도착역: " + txtGoEnd + "\n" +
                        "선택 월: " + selMonth + "\n" +
                        "선택 일: " + selDay + "\n" +
                        "시작 시간: " + String.format("%02d", startHour)  + "\n" +
                        "종료 시간: " + String.format("%02d", endTime.getHour()) + ":" + String.format("%02d", endTime.getMinute()) +
                        "https://www.korail.com/ 접속해서 결제해주세요"+ "\n\n";
                emailService.sendSimpleEmail(defaultRecipientEmail, subject, body);
            }
        }
        return isTicketFound;
    }

    // Ticket search criteria setting
    private void setTicketSearchCriteria(WebDriver driver, WebDriverWait wait, String txtGoStart, String txtGoEnd,
                                         Integer startHour, String selMonth, String selDay) {
//        WebElement startElement = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("start")));
//        startElement.clear();
//        startElement.sendKeys(txtGoStart);
//
//        WebElement endElement = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("get")));
//        endElement.clear();
//        endElement.sendKeys(txtGoEnd);
//        new Select(driver.findElement(By.name("selGoMonth"))).selectByValue(selMonth);
//        new Select(driver.findElement(By.name("selGoDay"))).selectByValue(selDay);
//        new Select(driver.findElement(By.name("selGoHour"))).selectByValue(String.format("%02d", startHour));

    }

    // Check ticket available for reservation
    private boolean findAndReserveAvailableTicket(WebDriver driver,  LocalTime startTime,
                                                  LocalTime endTime) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofMillis(100));
        List<WebElement> trainItems = driver.findElements(By.cssSelector("div.tabPage.active ul > li.tckList"));

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");


        for (WebElement train : trainItems) {
            // Get text in the form of "(09:38 ~ 10:27)"
            String timeRangeText = train.findElement(By.cssSelector("div.data_box.right h3 span:nth-child(3)")).getText();

            // Clean up and split the time range
            String cleanRange = timeRangeText.replaceAll("[()]", "").trim(); // "09:38 ~ 10:27"
            String[] times = cleanRange.split("~");

            // Parse departure and arrival time
            LocalTime departureTime = LocalTime.parse(times[0].trim(), timeFormatter);
            LocalTime arrivalTime = LocalTime.parse(times[1].trim(), timeFormatter);

            // Example output

            if (departureTime.isAfter(startTime) && arrivalTime.isBefore(endTime)) {
                // Find the reservation button

                try {
                    WebElement generalSeat = train.findElement(By.xpath(".//p[starts-with(text(), '일반실')]"));
                    generalSeat.click();  // Click only if found
                    handleReservationModal(driver, wait);
                    return true;  // Exit loop if you only want to click the first available one
                } catch (Exception e) {
                    // This train doesn't have '일반실', skip
                    continue;
                }
            }
        }
//        for (int i = 1; i < trainItems.size(); i++) {
//            WebElement startTimeText = driver.findElement(
//                    By.xpath("//*[@id='tableResult']/tbody/tr[" + i + "]/td[3]"));
//            String startTimeTextContent = startTimeText.getText().trim();
//            String timeString = startTimeTextContent.split("\\s+")[1];
//            LocalTime startTimeTextLocal = LocalTime.parse(timeString);
//
//            LocalTime currentTime = LocalTime.now();
//
//            if (startTimeTextLocal.isAfter(startTime) && startTimeTextLocal.isBefore(endTime)) {
//                long minutesDifference = Math.abs(Duration.between(currentTime, startTimeTextLocal).toMinutes());
//
//                if (minutesDifference > 20) {
//                    WebElement reservationButton = findReservationButton(driver, i);
//                    if (reservationButton != null) {
//                        reservationButton.click();
//
//                        try {
//                            handleReservationModal(driver, wait);
//                        } catch (Exception e) {
//                            return true;
//                        }
//
//                        return true;
//                    }
//                }
//            }
//        }
        return false;
    }

    // Handling in case of reservation modal
    private void handleReservationModal(WebDriver driver, WebDriverWait wait) {
        WebElement reserveButton = driver.findElement(By.xpath("//div[@class='ticket_reserv_wrap']//button[contains(@class, 'reservbtn')]"));
        reserveButton.click();
        close_ok_window(driver);
    }

    // Find btn to reserve
    private WebElement findReservationButton(WebDriver driver, int index) {
        try {
            WebElement regularSeatElement = driver.findElement(
                    By.xpath("//*[@id='tableResult']/tbody/tr[" + index + "]/td[6]"));
            WebElement specialSeatElement = driver.findElement(
                    By.xpath("//*[@id='tableResult']/tbody/tr[" + index + "]/td[5]"));

            WebElement regularSeatImg = regularSeatElement.findElement(By.tagName("img"));
            WebElement specialSeatImg = specialSeatElement.findElement(By.tagName("img"));

            String altRegular = regularSeatImg.getAttribute("alt");
            String altSpecial = specialSeatImg.getAttribute("alt");

            if (altRegular.equals("예약하기") && altSpecial.equals("예약하기")) {
                return regularSeatElement.findElement(By.tagName("a"));
            } else if (altRegular.equals("예약하기")) {
                return regularSeatElement.findElement(By.tagName("a"));
            } else if (altSpecial.equals("예약하기")) {
                return specialSeatElement.findElement(By.tagName("a"));
            } else {
                return null;
            }
        } catch (NoSuchElementException e) {
            return null;
        }
    }
}