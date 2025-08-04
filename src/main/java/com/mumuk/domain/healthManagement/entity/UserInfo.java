package com.mumuk.domain.healthManagement.entity;

import com.mumuk.domain.user.entity.User;
import com.mumuk.global.common.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name="user_info")
public class UserInfo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Gender gender=Gender.NONE;

    @Column(nullable = false)
    private Long height;
    @Column(nullable = false)
    private Long weight;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id")
    private User user;


    //생성자
    public UserInfo() {};
    public UserInfo(Long id, Gender gender, Long height, Long weight, User user) {
        this.id = id;
        this.gender = gender;
        this.height = height;
        this.weight = weight;
    }

    //getter
    public Long getId() {return id;}
    public Gender getGender() {return gender;}
    public Long getHeight() {return height;}
    public Long getWeight() {return weight;}
    public User getUser() {return user;}

    //setter
    public void setUser(User user) {this.user = user;}
    public void setGender(Gender gender) {this.gender = gender;}
    public void setHeight(Long height) {this.height = height;}
    public void setWeight(Long weight) {this.weight = weight;}

}
