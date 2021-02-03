package msacoffeechainsample;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;

@Entity
@Table(name="CustomerInfo_table")
public class CustomerInfo {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private String name;
    private Integer score;
    private String level;

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public Integer getScore() {
        return score;
    }
    public void setScore(Integer score) {
        this.score = score;
    }

    public String getLevel() {
        return level;
    }
    public void setLevel(String level) {
        this.level = level;
    }

}
