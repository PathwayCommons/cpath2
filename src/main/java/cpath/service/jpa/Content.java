package cpath.service.jpa;

import javax.persistence.*;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.util.Assert;

/**
 * A bio pathway/network data file from some data provider.
 */
@Deprecated //TODO: remove, refactor to simply use strings (path) in Metadata.content
@Entity
@DynamicUpdate
@DynamicInsert
@Table(name = "content", uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "filename"}))
public final class Content {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String filename;

  @Column(nullable = false)
  private String provider;


  /**
   * Default Constructor (for persistence)
   */
  public Content() {
  }


  /**
   * Create a Content domain object (value object).
   *
   * @param provider must be output provider for normalized data and validation reports
   * @param filename file name base (prefix for the normalized data and validation report file names)
   */
  public Content(Metadata provider, String filename) {
    Assert.notNull(provider, "provider cannot be null");
    Assert.notNull(filename, "filename cannot be null");
    this.provider = provider.getIdentifier();
    this.filename = filename.replaceAll("[^a-zA-Z0-9.-]", "_");
  }

  public String getProvider() {
    return provider;
  }

  @Override
  public String toString() {
    return provider + "/" + filename;
  }


  public String getFilename() {
    return filename;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Content) {
      final Content that = (Content) obj;
      return new EqualsBuilder().append(filename, that.getFilename()).append(provider, that.provider).isEquals();
    } else
      return false;
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(filename).append(provider).toHashCode();
  }
}
