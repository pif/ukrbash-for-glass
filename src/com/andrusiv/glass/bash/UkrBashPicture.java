package com.andrusiv.glass.bash;


public class UkrBashPicture {
	public static final UkrBashPicture NO_PICTURE = new UkrBashPicture("", "Нема що показати...", "Nothing to show now...");

	public final String url;
	public final String text;
	public final String translation;
	
	public UkrBashPicture(String url, String text, String translation) {
		this.url = url;
		this.text = text;
		this.translation = translation;
	}
	
	public String toHtml() {
		return generateHtml(this);
	}
	
	private static String generateHtml(UkrBashPicture picture) {
		String html = "" +
//				"<article>" +
				"<article class=\"photo\">" +
				"  <img src=\""+picture.url+"\" width=\"100%\" height=\"100%\" />" +
// TODO on my Nexus Glass this line causes text section to disappear...
//				"  <div class=\"photo-overlay\" />" +
				"  <section>" +
				"    <p class=\"text-auto-size\" style=\"visibility: visible\">"+picture.text+"</p>" +
				"  </section>" +
				"</article>";
		return html;
	}
}
