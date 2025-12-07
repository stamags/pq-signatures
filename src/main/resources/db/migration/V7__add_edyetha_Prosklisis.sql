DROP TABLE IF EXISTS `edyetha_prosklisis`;

CREATE TABLE `edyetha_prosklisis`(
  `ID` INT(11) NOT NULL AUTO_INCREMENT,
  `EDYETHA` VARCHAR(250),
  PRIMARY KEY (`ID`)
) ENGINE=INNODB CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

insert  into `edyetha_prosklisis`(`ID`,`EDYETHA`) values (1,'ΕΔΥΕΘΑ')