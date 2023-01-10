package de.fraunhofer.scai.bio.owltooling;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

/**
 * language specific labels
 * 
 * @author Marc Jacobs
 *
 */
public class Label {

	@JsonProperty("language")
	  @Getter @Setter private String language = null;

	  @JsonProperty("content")
	  @Getter @Setter private String content = null;
	  
	  public Label(String content, String language) {
		  setContent(content);
		  setLanguage(language);
	  }
	  
	  @Override
	  public boolean equals(java.lang.Object o) {
	    if (this == o) {
	      return true;
	    }
	    if (o == null || getClass() != o.getClass()) {
	      return false;
	    }
	    Label label = (Label) o;
	    return Objects.equals(this.content, label.content) &&
	        Objects.equals(this.language, label.language);
	  }

	  @Override
	  public int hashCode() {
	    return Objects.hash(language, content);
	  }

	  @Override
	  public String toString() {
	    StringBuilder sb = new StringBuilder();
	    sb.append("\"");
	    sb.append(content);
	    sb.append("\"@");
	    sb.append(language);    
	    return sb.toString();
	  }

}
