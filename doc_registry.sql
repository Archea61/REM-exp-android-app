-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Хост: 127.0.0.1:3307
-- Время создания: Фев 16 2026 г., 18:44
-- Версия сервера: 10.4.32-MariaDB
-- Версия PHP: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- База данных: `doc_registry`
--
CREATE DATABASE IF NOT EXISTS `doc_registry` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `doc_registry`;

DELIMITER $$
--
-- Процедуры
--
DROP PROCEDURE IF EXISTS `filter_by_status`$$
CREATE DEFINER=`root`@`localhost` PROCEDURE `filter_by_status` (IN `p_status` VARCHAR(255))   BEGIN
    SELECT d.id,
           d.reg_number,
           DATE_FORMAT(d.reg_date, '%Y-%m-%d') AS reg_date,
           dt.name AS type_name,
           d.title,
           d.status,
           DATE_FORMAT(d.due_date, '%Y-%m-%d') AS due_date
    FROM `document` d
    LEFT JOIN doc_type dt ON d.type_id = dt.id
    WHERE d.status = p_status
    ORDER BY d.id DESC;
END$$

DROP PROCEDURE IF EXISTS `filter_by_type`$$
CREATE DEFINER=`root`@`localhost` PROCEDURE `filter_by_type` (IN `p_type_name` VARCHAR(255))   BEGIN
    SELECT d.id,
           d.reg_number,
           DATE_FORMAT(d.reg_date, '%Y-%m-%d') AS reg_date,
           dt.name AS type_name,
           d.title,
           d.status,
           DATE_FORMAT(d.due_date, '%Y-%m-%d') AS due_date
    FROM `document` d
    LEFT JOIN doc_type dt ON d.type_id = dt.id
    WHERE dt.name = p_type_name
    ORDER BY d.id DESC;
END$$

DROP PROCEDURE IF EXISTS `get_completed_docs`$$
CREATE DEFINER=`root`@`localhost` PROCEDURE `get_completed_docs` ()   BEGIN
    SELECT d.id,
           d.reg_number,
           DATE_FORMAT(d.reg_date, '%Y-%m-%d') AS reg_date,
           dt.name AS type_name,
           d.title,
           d.status,
           DATE_FORMAT(d.due_date, '%Y-%m-%d') AS due_date
    FROM `document` d
    LEFT JOIN doc_type dt ON d.type_id = dt.id
    WHERE LOWER(d.status) = 'выполнен'
    ORDER BY d.id DESC;
END$$

DROP PROCEDURE IF EXISTS `get_overdue_docs`$$
CREATE DEFINER=`root`@`localhost` PROCEDURE `get_overdue_docs` ()   BEGIN
    SELECT d.id,
           d.reg_number,
           DATE_FORMAT(d.reg_date, '%Y-%m-%d') AS reg_date,
           dt.name AS type_name,
           d.title,
           d.status,
           DATE_FORMAT(d.due_date, '%Y-%m-%d') AS due_date
    FROM `document` d
    LEFT JOIN doc_type dt ON d.type_id = dt.id
    WHERE d.due_date < CURDATE()
      AND (d.status IS NULL OR LOWER(d.status) <> 'выполнен')
    ORDER BY d.due_date ASC;
END$$

DROP PROCEDURE IF EXISTS `search_documents_by_title`$$
CREATE DEFINER=`root`@`localhost` PROCEDURE `search_documents_by_title` (IN `p_term` VARCHAR(255))   BEGIN
    SELECT d.id,
           d.reg_number,
           DATE_FORMAT(d.reg_date, '%Y-%m-%d') AS reg_date,
           dt.name AS type_name,
           d.title,
           d.status,
           DATE_FORMAT(d.due_date, '%Y-%m-%d') AS due_date
    FROM `document` d
    LEFT JOIN doc_type dt ON d.type_id = dt.id
    WHERE d.title LIKE CONCAT('%', p_term, '%')
    ORDER BY d.id DESC;
END$$

DELIMITER ;

-- --------------------------------------------------------

--
-- Структура таблицы `department`
--

DROP TABLE IF EXISTS `department`;
CREATE TABLE `department` (
  `id` int(11) NOT NULL,
  `name` varchar(150) NOT NULL,
  `chief` varchar(150) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Структура таблицы `document`
--

DROP TABLE IF EXISTS `document`;
CREATE TABLE `document` (
  `id` int(11) NOT NULL,
  `reg_number` varchar(50) DEFAULT NULL,
  `reg_date` date DEFAULT NULL,
  `type_id` int(11) DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL,
  `author_id` int(11) DEFAULT NULL,
  `receiver` varchar(255) DEFAULT NULL,
  `status` varchar(50) DEFAULT NULL,
  `due_date` date DEFAULT NULL,
  `file_path` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Триггеры `document`
--
DROP TRIGGER IF EXISTS `trg_document_autonumber`;
DELIMITER $$
CREATE TRIGGER `trg_document_autonumber` BEFORE INSERT ON `document` FOR EACH ROW BEGIN
    IF NEW.reg_number IS NULL OR NEW.reg_number = '' THEN
        SET NEW.reg_number = CONCAT(YEAR(CURDATE()), '-', (SELECT IFNULL(MAX(id),0)+1 FROM document));
    END IF;
END
$$
DELIMITER ;
DROP TRIGGER IF EXISTS `trg_document_due_validation`;
DELIMITER $$
CREATE TRIGGER `trg_document_due_validation` BEFORE INSERT ON `document` FOR EACH ROW BEGIN
    IF NEW.due_date IS NOT NULL AND NEW.reg_date IS NOT NULL THEN
        IF NEW.due_date < NEW.reg_date THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Ошибка: due_date не может быть раньше reg_date';
        END IF;
    END IF;
END
$$
DELIMITER ;
DROP TRIGGER IF EXISTS `trg_document_status_update`;
DELIMITER $$
CREATE TRIGGER `trg_document_status_update` AFTER UPDATE ON `document` FOR EACH ROW BEGIN
    IF NEW.status <> OLD.status THEN
        INSERT INTO log_record (document_id, user_id, action)
        VALUES (NEW.id, NEW.author_id, CONCAT('Статус изменён на "', NEW.status, '"'));
    END IF;
END
$$
DELIMITER ;

-- --------------------------------------------------------

--
-- Структура таблицы `doc_executor`
--

DROP TABLE IF EXISTS `doc_executor`;
CREATE TABLE `doc_executor` (
  `id` int(11) NOT NULL,
  `document_id` int(11) DEFAULT NULL,
  `employee_id` int(11) DEFAULT NULL,
  `exec_status` varchar(50) DEFAULT NULL,
  `comment` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Триггеры `doc_executor`
--
DROP TRIGGER IF EXISTS `trg_executor_insert`;
DELIMITER $$
CREATE TRIGGER `trg_executor_insert` AFTER INSERT ON `doc_executor` FOR EACH ROW BEGIN
    INSERT INTO log_record(document_id, user_id, action)
    VALUES (NEW.document_id, NEW.employee_id,
            CONCAT('Назначен исполнитель (ID сотрудника ', NEW.employee_id, ')'));
END
$$
DELIMITER ;
DROP TRIGGER IF EXISTS `trg_executor_update`;
DELIMITER $$
CREATE TRIGGER `trg_executor_update` AFTER UPDATE ON `doc_executor` FOR EACH ROW BEGIN
    DECLARE cnt_total INT;
    DECLARE cnt_done INT;

    IF NEW.exec_status <> OLD.exec_status THEN

        SELECT COUNT(*) INTO cnt_total
        FROM doc_executor
        WHERE document_id = NEW.document_id;

        SELECT COUNT(*) INTO cnt_done
        FROM doc_executor
        WHERE document_id = NEW.document_id
          AND LOWER(exec_status) = 'выполнено';

        IF cnt_total = cnt_done AND cnt_total > 0 THEN
            UPDATE document
            SET status = 'выполнен'
            WHERE id = NEW.document_id;
        END IF;

    END IF;
END
$$
DELIMITER ;

-- --------------------------------------------------------

--
-- Структура таблицы `doc_type`
--

DROP TABLE IF EXISTS `doc_type`;
CREATE TABLE `doc_type` (
  `id` int(11) NOT NULL,
  `name` varchar(100) NOT NULL,
  `description` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Структура таблицы `employee`
--

DROP TABLE IF EXISTS `employee`;
CREATE TABLE `employee` (
  `id` int(11) NOT NULL,
  `full_name` varchar(150) NOT NULL,
  `position` varchar(100) DEFAULT NULL,
  `department_id` int(11) DEFAULT NULL,
  `login` varchar(50) DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL,
  `access_level` int(11) DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Структура таблицы `log_record`
--

DROP TABLE IF EXISTS `log_record`;
CREATE TABLE `log_record` (
  `id` int(11) NOT NULL,
  `document_id` int(11) DEFAULT NULL,
  `user_id` int(11) DEFAULT NULL,
  `action` varchar(100) DEFAULT NULL,
  `timestamp` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Структура таблицы `numbers`
--

DROP TABLE IF EXISTS `numbers`;
CREATE TABLE `numbers` (
  `n` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Индексы сохранённых таблиц
--

--
-- Индексы таблицы `department`
--
ALTER TABLE `department`
  ADD PRIMARY KEY (`id`);

--
-- Индексы таблицы `document`
--
ALTER TABLE `document`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `reg_number` (`reg_number`),
  ADD KEY `author_id` (`author_id`),
  ADD KEY `idx_document_type` (`type_id`),
  ADD KEY `idx_document_status` (`status`),
  ADD KEY `idx_document_due` (`due_date`);

--
-- Индексы таблицы `doc_executor`
--
ALTER TABLE `doc_executor`
  ADD PRIMARY KEY (`id`),
  ADD KEY `document_id` (`document_id`),
  ADD KEY `employee_id` (`employee_id`);

--
-- Индексы таблицы `doc_type`
--
ALTER TABLE `doc_type`
  ADD PRIMARY KEY (`id`);

--
-- Индексы таблицы `employee`
--
ALTER TABLE `employee`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `login` (`login`),
  ADD KEY `department_id` (`department_id`);

--
-- Индексы таблицы `log_record`
--
ALTER TABLE `log_record`
  ADD PRIMARY KEY (`id`),
  ADD KEY `document_id` (`document_id`),
  ADD KEY `user_id` (`user_id`);

--
-- Индексы таблицы `numbers`
--
ALTER TABLE `numbers`
  ADD PRIMARY KEY (`n`);

--
-- AUTO_INCREMENT для сохранённых таблиц
--

--
-- AUTO_INCREMENT для таблицы `department`
--
ALTER TABLE `department`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT для таблицы `document`
--
ALTER TABLE `document`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT для таблицы `doc_executor`
--
ALTER TABLE `doc_executor`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT для таблицы `doc_type`
--
ALTER TABLE `doc_type`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT для таблицы `employee`
--
ALTER TABLE `employee`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT для таблицы `log_record`
--
ALTER TABLE `log_record`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- Ограничения внешнего ключа сохраненных таблиц
--

--
-- Ограничения внешнего ключа таблицы `document`
--
ALTER TABLE `document`
  ADD CONSTRAINT `document_ibfk_1` FOREIGN KEY (`type_id`) REFERENCES `doc_type` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `document_ibfk_2` FOREIGN KEY (`author_id`) REFERENCES `employee` (`id`) ON DELETE SET NULL;

--
-- Ограничения внешнего ключа таблицы `doc_executor`
--
ALTER TABLE `doc_executor`
  ADD CONSTRAINT `doc_executor_ibfk_1` FOREIGN KEY (`document_id`) REFERENCES `document` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `doc_executor_ibfk_2` FOREIGN KEY (`employee_id`) REFERENCES `employee` (`id`) ON DELETE CASCADE;

--
-- Ограничения внешнего ключа таблицы `employee`
--
ALTER TABLE `employee`
  ADD CONSTRAINT `employee_ibfk_1` FOREIGN KEY (`department_id`) REFERENCES `department` (`id`) ON DELETE SET NULL;

--
-- Ограничения внешнего ключа таблицы `log_record`
--
ALTER TABLE `log_record`
  ADD CONSTRAINT `log_record_ibfk_1` FOREIGN KEY (`document_id`) REFERENCES `document` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `log_record_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `employee` (`id`) ON DELETE SET NULL;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
