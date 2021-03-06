package com.github.invictum.allure;

import com.github.invictum.allure.annotation.AnnotationUtil;
import com.github.invictum.allure.events.StepPendingEvent;
import com.github.invictum.allure.events.TestCaseCanceledWithMessageEvent;
import com.github.invictum.allure.issue.ClassIssueProcessor;
import com.github.invictum.allure.issue.IssueProcessor;
import com.github.invictum.allure.issue.StoryIssueProcessor;
import com.github.invictum.allure.utils.EnvironmentUtil;
import com.github.invictum.allure.utils.ScreenshotUtil;
import net.thucydides.core.model.DataTable;
import net.thucydides.core.model.Story;
import net.thucydides.core.model.TestOutcome;
import net.thucydides.core.steps.ExecutedStepDescription;
import net.thucydides.core.steps.StepFailure;
import net.thucydides.core.steps.StepListener;
import ru.yandex.qatools.allure.Allure;
import ru.yandex.qatools.allure.events.*;

import java.util.Map;
import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.EMPTY;

public class AllureStepListener implements StepListener {

    private Allure allure = Allure.LIFECYCLE;
    private String suitUid = EMPTY;
    private boolean titleTransformationRequired = true;
    private IssueProcessor issueProcessor;

    public AllureStepListener() {
        EnvironmentUtil.create();
    }

    private String getSuitUid() {
        suitUid = UUID.randomUUID().toString();
        return suitUid;
    }

    private void makeAttachmentIfPossible(String title) {
        byte[] content = ScreenshotUtil.takeScreenshotContent();
        if (content != null) {
            allure.fire(new MakeAttachmentEvent(content, title, "image/png"));
        }
    }

    @Override
    public void testSuiteStarted(Class<?> storyClass) {
        issueProcessor = new ClassIssueProcessor(storyClass);
        TestSuiteStartedEvent event = new TestSuiteStartedEvent(getSuitUid(), storyClass.getSimpleName());
        event = AnnotationUtil.withIssues(event, issueProcessor);
        allure.fire(AnnotationUtil.withClass(event, storyClass));
    }

    @Override
    public void testSuiteStarted(Story story) {
        issueProcessor = new StoryIssueProcessor(story);
        TestSuiteStartedEvent event = new TestSuiteStartedEvent(getSuitUid(), story.getStoryName());
        allure.fire(AnnotationUtil.withStory(event, story));
        titleTransformationRequired = false;
    }

    @Override
    public void testSuiteFinished() {
        allure.fire(new TestSuiteFinishedEvent(suitUid));
        titleTransformationRequired = true;
    }

    @Override
    public void testStarted(String description) {
        TestCaseStartedEvent event = AnnotationUtil.withEssentialInfo(new TestCaseStartedEvent(suitUid, description));
        if (titleTransformationRequired) {
            event = AnnotationUtil.withTitle(event);
        }
        event = AnnotationUtil.withIssues(event, issueProcessor);
        allure.fire(AnnotationUtil.withEssentialInfo(event));
    }

    @Override
    public void testStarted(String description, String id) {
        TestCaseStartedEvent event = new TestCaseStartedEvent(suitUid, description);
        event = AnnotationUtil.withIssues(event, issueProcessor);
        allure.fire(AnnotationUtil.withEssentialInfo(event));
    }

    @Override
    public void testFinished(TestOutcome result) {
        if (result.isError() || result.isFailure()) {
            TestCaseFailureEvent event = new TestCaseFailureEvent();
            event.withThrowable(result.getTestFailureCause().toException());
            allure.fire(event);
        }
        allure.fire(new TestCaseFinishedEvent());
    }

    @Override
    public void testRetried() {
        //TODO: Investigate how to integrate it.
    }

    @Override
    public void stepStarted(ExecutedStepDescription description) {
        allure.fire(new StepStartedEvent(description.getTitle()));
        makeAttachmentIfPossible("Step started");
    }

    @Override
    public void skippedStepStarted(ExecutedStepDescription description) {
        allure.fire(new StepStartedEvent(description.getTitle()));
    }

    @Override
    public void stepFailed(StepFailure failure) {
        allure.fire(new StepFailureEvent().withThrowable(failure.getException()));
        //Attachment should be proceed only inside step?
        makeAttachmentIfPossible(String.format("Step failed: %s", failure.getMessage()));
        allure.fire(new StepFinishedEvent());
    }

    @Override
    public void lastStepFailed(StepFailure failure) {
        //Isn't used in allure.
    }

    @Override
    public void stepIgnored() {
        allure.fire(new StepCanceledEvent());
        allure.fire(new StepFinishedEvent());
    }

    @Override
    public void stepPending() {
        allure.fire(new StepPendingEvent());
        allure.fire(new StepFinishedEvent());
    }

    @Override
    public void stepPending(String message) {
        allure.fire(new StepPendingEvent());
        allure.fire(new StepFinishedEvent());
    }

    @Override
    public void stepFinished() {
        makeAttachmentIfPossible("Step finished");
        allure.fire(new StepFinishedEvent());
    }

    @Override
    public void testFailed(TestOutcome testOutcome, Throwable cause) {
        //Method used only with jUnit. Let's handle fail in testFinished method.
    }

    @Override
    public void testIgnored() {
        allure.fire(new TestCaseCanceledWithMessageEvent().withMessage("Test was marked as ignored"));
        allure.fire(new TestCaseFinishedEvent());
    }

    @Override
    public void testSkipped() {
        //TODO: Investigate how to integrate it.
    }

    @Override
    public void testPending() {
        allure.fire(new TestCasePendingEvent());
    }

    @Override
    public void testIsManual() {
        //Not supported in Allure.
    }

    @Override
    public void notifyScreenChange() {
        //Isn't used in allure.
    }

    @Override
    public void useExamplesFrom(DataTable table) {
        //Isn't used in allure.
    }

    @Override
    public void addNewExamplesFrom(DataTable table) {
        //Isn't used in allure.
    }

    @Override
    public void exampleStarted(Map<String, String> data) {
        //Isn't used in allure.
    }

    @Override
    public void exampleFinished() {
        //Isn't used in allure.
    }

    @Override
    public void assumptionViolated(String message) {
        //Isn't used in allure.
    }

    @Override
    public void testRunFinished() {
        //Isn't used in allure.
    }
}
