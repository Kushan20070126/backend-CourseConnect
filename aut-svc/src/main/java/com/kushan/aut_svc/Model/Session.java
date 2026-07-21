package com.kushan.aut_svc.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Table(name = "sessions", indexes = {
		@Index(name = "idx_sessions_user", columnList = "user_id"),
		@Index(name = "idx_sessions_jti", columnList = "jti")
})
public class Session {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "jti", nullable = false, unique = true)
	private String jti;

	@Column(name = "ip_address")
	private String ipAddress;

	@Column(name = "device_type")
	private String deviceType;

	@Column(name = "browser")
	private String browser;

	@Column(name = "os")
	private String os;

	@Column(name = "user_agent")
	private String userAgent;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "last_seen", nullable = false)
	private LocalDateTime lastSeen;

	@Column(name = "expires_at")
	private LocalDateTime expiresAt;

	@Column(name = "active", nullable = false)
	private boolean active = true;

	public Session() {
	}

}
