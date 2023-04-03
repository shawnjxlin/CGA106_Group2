package com.ezticket.web.users.pojo;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "HOST")
public class Host {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "HOSTNO")
    private Integer hostno;
    @Column(name = "HOSTNAME")
    private String hostname;
    @Column(name = "HOSTCONTACT")
    private String hostcontact;
    @Column(name = "HOSTEMAIL")
    private String hostemail;
    @Column(name = "HOSTCELL")
    private String hostcell;
}