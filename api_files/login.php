<?php
header('Content-Type: application/json');
require_once 'config.php';

$response = array();

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    $token = $_POST['recaptcha_token'] ?? '';
    if (empty($token)) {
        $received = implode(', ', array_keys($_POST));
        echo json_encode(['success' => false, 'message' => 'DEBUG - params received: [' . $received . ']']);
        exit();
    }

    $api_key  = '******************';
    $project  = '******************';
    $site_key = '******************';
    $url      = "https://recaptchaenterprise.googleapis.com/v1/projects/{$project}/assessments?key={$api_key}";

    $body = json_encode([
        'event' => [
            'token'          => $token,
            'siteKey'        => $site_key,
            'expectedAction' => 'LOGIN'
        ]
    ]);

    $opts      = ['http' => ['method' => 'POST', 'header' => 'Content-Type: application/json', 'content' => $body, 'ignore_errors' => true]];
    $rawResult = @file_get_contents($url, false, stream_context_create($opts));
    $result    = ($rawResult !== false) ? json_decode($rawResult, true) : null;

    if (!$result || empty($result['tokenProperties']['valid']) || ($result['riskAnalysis']['score'] ?? 0) < 0.3) {
        $score = $result['riskAnalysis']['score'] ?? 'N/A';
        log_warn('login', "reCAPTCHA failed - score:{$score}");
        echo json_encode(['success' => false, 'message' => 'Security check failed. Please try again.']);
        exit();
    }

    $email    = $_POST['email'];
    $password = $_POST['password'];

    $stmt = $conn->prepare("SELECT user_id, username, password FROM users WHERE email = ?");
    $stmt->bind_param("s", $email);
    $stmt->execute();
    $result = $stmt->get_result();

    if ($result->num_rows == 1) {
        $user = $result->fetch_assoc();
        if (password_verify($password, $user['password'])) {
            $response['success']  = true;
            $response['user_id']  = $user['user_id'];
            $response['username'] = $user['username'];
            $response['message']  = "Login successful";
            log_info('login', "Login successful - user_id:{$user['user_id']} username:{$user['username']}");
        } else {
            $response['success'] = false;
            $response['message'] = "Invalid password";
            log_warn('login', "Invalid password for email:{$email}");
        }
    } else {
        $response['success'] = false;
        $response['message'] = "User not found";
        log_warn('login', "User not found for email:{$email}");
    }
} else {
    $response['success'] = false;
    $response['message'] = "Invalid request method";
}

echo json_encode($response);
?>
