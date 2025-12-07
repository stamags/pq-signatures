DROP TABLE IF EXISTS `sypo_version`;

CREATE TABLE `sypo_version`(
  `ID` INT(11) NOT NULL AUTO_INCREMENT,
  `HMEROMHNIA` DATE COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `VERSION` VARCHAR(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `PERIGRAFH` VARCHAR(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

insert  into `sypo_version`(`ID`,`HMEROMHNIA`,`VERSION`,`PERIGRAFH`) values (1,'2020-10-06 00:00:00','1.00','Άρχική Έκδοση')