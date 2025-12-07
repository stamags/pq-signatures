update ken_ikanothtes set IKAN_EFED_KWD = 1 where IKAN_EFED_KWD is null;

update ken_ikanothtes set ken_ikanothtes.IKAN_HMNIA = '1900-01-01' where ken_ikanothtes.IKAN_HMNIA is null;

update ken_ikanothtes set IKAN_AR_GNWMAT = 0 where IKAN_AR_GNWMAT is null;

update ken_ikanothtes set IKAN_PD_PARAG = 0 where IKAN_PD_PARAG is null;

ALTER TABLE `kends`.`ken_ikanothtes`
  CHANGE `IKAN_EFED_KWD` `IKAN_EFED_KWD` INT(11) NOT NULL,
  CHANGE `IKAN_HMNIA` `IKAN_HMNIA` DATE NOT NULL,
  CHANGE `IKAN_AR_GNWMAT` `IKAN_AR_GNWMAT` INT(11) NOT NULL,
  CHANGE `IKAN_PD_PARAG` `IKAN_PD_PARAG` INT(11) NOT NULL,
  DROP PRIMARY KEY,
  ADD PRIMARY KEY (`STRK_PROS_KWD`, `IKAN_EFED_KWD`, `IKAN_HMNIA`, `IKAN_AR_GNWMAT`, `IKAN_PD_PARAG`);

ALTER TABLE `kends`.`ken_ikanothtes` DROP FOREIGN KEY `FK_ken_ikanothtes_ikanothta_efedrou`;

ALTER TABLE `kends`.`ken_ikanothtes` ADD CONSTRAINT `FK_ken_ikanothtes_ikanothta_efedrou` FOREIGN KEY (`IKAN_EFED_KWD`) REFERENCES `kends`.`ikanothta_efedrou`(`IKAN_EFED_KWD`) ON UPDATE CASCADE ON DELETE CASCADE;

ALTER TABLE `kends`.`ken_ikanothtes` DROP FOREIGN KEY `FK_ken_ikanothtes_pd_133_02`;

ALTER TABLE `kends`.`ken_ikanothtes` ADD CONSTRAINT `FK_ken_ikanothtes_pd_133_02` FOREIGN KEY (`IKAN_PD_PARAG`) REFERENCES `kends`.`pd_133_02`(`PD_PARAG_KWD`) ON UPDATE CASCADE ON DELETE CASCADE;
