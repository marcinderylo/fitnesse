package fitnesse.responders.run.formatters;

import java.io.IOException;

import fitnesse.FitNesseContext;
import fitnesse.html.HtmlTag;
import fitnesse.html.HtmlUtil;
import fitnesse.html.RawHtml;
import fitnesse.responders.run.CompositeExecutionLog;
import fitnesse.responders.run.ExecutionStatus;
import fitnesse.responders.run.TestPage;
import fitnesse.responders.run.TestSummary;
import fitnesse.wiki.PageCrawler;
import fitnesse.wiki.WikiPage;

public abstract class InteractiveFormatter extends BaseFormatter {

  private static final String TESTING_INTERUPTED = "<strong>Testing was interupted and results are incomplete.</strong>";

  private boolean wasInterupted = false;
  private TestSummary assertionCounts = new TestSummary();

  private CompositeExecutionLog log;
  
  protected InteractiveFormatter() {
    super();
  }

  protected InteractiveFormatter(FitNesseContext context, WikiPage page) {
    super(context, page);
  }

  protected abstract void writeData(String output);

  protected void updateSummaryDiv(String html) {
    writeData(HtmlUtil.makeReplaceElementScript("test-summary", html).html());
  }

  protected String getRelativeName(TestPage testPage) {
    PageCrawler pageCrawler = getPage().getPageCrawler();
    String relativeName = pageCrawler.getRelativeName(getPage(), testPage.getSourcePage());
    if ("".equals(relativeName)) {
      relativeName = String.format("(%s)", testPage.getName());
    }
    return relativeName;
  }

  protected void addStopLink(String stopResponderId) {
    String link = "?responder=stoptest&id=" + stopResponderId;

    HtmlTag status = HtmlUtil.makeSilentLink(link, new RawHtml("Stop Test"));
    status.addAttribute("class", "stop");
    
    writeData(HtmlUtil.makeReplaceElementScript("test-action", status.html()).html());
  }

  protected void removeStopTestLink() {
    HtmlTag script = HtmlUtil.makeReplaceElementScript("test-action", "");
    writeData(script.html());
  }


  protected String cssClassFor(TestSummary testSummary) {
    if (testSummary.getWrong() > 0 || wasInterupted)
      return "fail";
    else if (testSummary.getExceptions() > 0
      || testSummary.getRight() + testSummary.getIgnores() == 0)
      return "error";
    else if (testSummary.getIgnores() > 0 && testSummary.getRight() == 0)
      return "ignore";
    else
      return "pass";
  }

  public TestSummary getAssertionCounts() {
    return assertionCounts;
  }

  public boolean wasInterupted() {
    return wasInterupted;
  }
  
  @Override
  public void errorOccured() {
    wasInterupted = true;
    super.errorOccured();
  }
  
  public String testSummary() {
    String summaryContent = (wasInterupted()) ? TESTING_INTERUPTED : "";
    summaryContent += makeSummaryContent();
    HtmlTag script = HtmlUtil.makeReplaceElementScript("test-summary", summaryContent);
    script.add("document.getElementById(\"test-summary\").className = \""
      + cssClassFor(getAssertionCounts()) + "\";");
    return script.html();
  }

  protected abstract String makeSummaryContent();

  protected void finishWritingOutput() throws IOException {
  }

  protected void close() {
  }
  
  @Override
  public void setExecutionLogAndTrackingId(String stopResponderId, CompositeExecutionLog log) {
    this.log = log;
    addStopLink(stopResponderId);
  }

  protected void publishAndAddLog() throws IOException {
    if (log != null) {
      log.publish();
      writeData(HtmlUtil.makeReplaceElementScript("test-action", executionStatus(log)).html());
    }
  }

  public String executionStatus(CompositeExecutionLog log) {
    String errorLogPageName = log.getErrorLogPageName();
    if (log.exceptionCount() != 0)
      return makeExecutionStatusLink(errorLogPageName, ExecutionStatus.ERROR);

    if (log.hasCapturedOutput())
      return makeExecutionStatusLink(errorLogPageName, ExecutionStatus.OUTPUT);

    return makeExecutionStatusLink(errorLogPageName, ExecutionStatus.OK);
  }
  
  public static String makeExecutionStatusLink(String linkHref, ExecutionStatus executionStatus) {
    HtmlTag status = HtmlUtil.makeLink(linkHref, executionStatus.getMessage());
    status.addAttribute("class", executionStatus.getStyle());
    return status.html();
  }

}
