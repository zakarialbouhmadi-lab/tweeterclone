<?php
define('LOG_DIR', dirname(__DIR__) . '/logs/');

function write_log($level, $context, $message) {
    $dir = LOG_DIR;
    if (!is_dir($dir)) {
        mkdir($dir, 0755, true);
    }

    $timestamp = date('Y-m-d H:i:s');
    $date      = date('Y-m-d');
    $ip        = $_SERVER['REMOTE_ADDR'] ?? 'unknown';
    $entry     = "[{$timestamp}] [{$level}] [{$context}] [IP:{$ip}] {$message}" . PHP_EOL;

    file_put_contents($dir . "api_{$date}.log", $entry, FILE_APPEND | LOCK_EX);
}

function log_info($context, $message)  { write_log('INFO',  $context, $message); }
function log_warn($context, $message)  { write_log('WARN',  $context, $message); }
function log_error($context, $message) { write_log('ERROR', $context, $message); }
