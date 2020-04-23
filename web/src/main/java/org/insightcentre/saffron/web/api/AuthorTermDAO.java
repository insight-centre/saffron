package org.insightcentre.saffron.web.api;

import org.insightcentre.nlp.saffron.data.Author;

public class AuthorTermDAO {
	
	private Author author;
	private double tfirf;
	
	public AuthorTermDAO() {
	
	}
	
	public AuthorTermDAO(Author author, double tfirf) {
		this.setAuthor(author);
		this.setTfirf(tfirf);
	}
	
	public Author getAuthor() {
		return author;
	}
	public void setAuthor(Author author) {
		this.author = author;
	}
	public double getTfirf() {
		return tfirf;
	}
	public void setTfirf(double tfirf) {
		this.tfirf = tfirf;
	}
}