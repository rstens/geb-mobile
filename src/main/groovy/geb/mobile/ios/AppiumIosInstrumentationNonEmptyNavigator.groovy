package geb.mobile.ios

import geb.Browser
import geb.Page
import geb.error.UndefinedAtCheckerException
import geb.error.UnexpectedPageException
import geb.mobile.AbstractMobileNonEmptyNavigator
import geb.navigator.AbstractNavigator
import geb.navigator.EmptyNavigator
import geb.navigator.Navigator
import geb.navigator.SelectFactory
import geb.textmatching.TextMatcher
import geb.waiting.WaitTimeoutException
import groovy.util.logging.Slf4j
import io.appium.java_client.AppiumDriver
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import sun.reflect.generics.reflectiveObjects.NotImplementedException

import java.util.regex.Pattern

import static java.util.Collections.EMPTY_LIST

/**
 * Created by gmueksch on 23.06.14.
 */
@Slf4j
class AppiumIosInstrumentationNonEmptyNavigator extends AbstractMobileNonEmptyNavigator<AppiumDriver> {

    AppiumIosInstrumentationNonEmptyNavigator(Browser browser, Collection<? extends WebElement> contextElements) {
        super(browser,contextElements)
    }

    static def pat = ~/(\w+)='?([\w,\- ]+)'?/

    @Override
    Navigator find(String selectorString) {
        log.debug "Selector: $selectorString"

        if( selectorString.startsWith("//") ) {
            return navigatorFor(browser.driver.findElements(By.xpath(selectorString)))
        }
        String all,key,value
        if (selectorString.startsWith("#")) {
            key = "id"
            value = selectorString.substring(1)
        } else {
            def m = pat.matcher(selectorString)
            if (m.matches()) {
                (all,key,value)=m[0]
                log.debug "Match for ${key}='${value}' in $selectorString"
            }
        }
        if( key && value ) {
            log.debug("Key:$key , Value: $value")
            navigatorFor browser.driver.findElements(By."$key"(value))
        }else{
            log.warn("Ether key '$key' or value '$value' is not filled")
            new EmptyNavigator()
        }
    }

    @Override
    protected getInputValue(WebElement input) {
        def value = null
        def type = input.getAttribute("type")
        if (input.tagName == "select") {
            def select = new SelectFactory().createSelectFor(input)
            if (select.multiple) {
                value = select.allSelectedOptions.collect { getValue(it) }
            } else {
                value = getValue(select.firstSelectedOption)
            }
        } else if (type in ["checkbox", "radio"]) {
            if (input.isSelected()) {
                value = getValue(input)
            } else {
                if (type == "checkbox") {
                    value = false
                }
            }
        } else {
            value = getValue(input)
        }
        value
    }

    @Override
    void setInputValue(WebElement input, value) {
        def attrType = input.getAttribute("type")
        if (attrType == "UIASelect") {
            setSelectValue(input, value)
        } else if (attrType == "UIACheckbox") {
            if (getValue(input) == value.toString() || value == true) {
                if (!input.isSelected()) {
                    input.click()
                }
            } else if (input.isSelected()) {
                input.click()
            }
        } else if (attrType == "UIARadio") {
            if (getValue(input) == value.toString() || labelFor(input) == value.toString()) {
                input.click()
            }
        } else {
            input.clear()
            input.sendKeys value as String
        }
    }

    protected getValue(WebElement input) {
        input?.getAttribute("value")
    }


    @Override
    boolean isDisabled() {
        def dis = _props.enabled
        if( dis == null ){
            dis = _props.disabled
        }
        return dis
    }

    @Override
    Navigator unique() {
        new AppiumIosInstrumentationNonEmptyNavigator(browser, contextElements.unique(false))
    }
}