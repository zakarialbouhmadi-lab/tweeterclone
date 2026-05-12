<?php
header('Content-Type: application/json');
require_once 'config.php';

$response = array();

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    $token = $_POST['recaptcha_token'] ?? '';
    if (empty($token)) {
        echo json_encode(['success' => false, 'message' => 'Security check missing']);
        exit();
    }

    $api_key  = 'AIzaSyAmoURlho4X8vzkDfn7oVvPbf7F_pYeK1k';
    $project  = 'tweeterclone-496100';
    $site_key = '6LflauUsAAAAADnhveCokPOvE4Cq-OnxJlgl1dnc';
    $url      = "https://recaptchaenterprise.googleapis.com/v1/projects/{$project}/assessments?key={$api_key}";

    $body = json_encode([
        'event' => [
            'token'          => $token,
            'siteKey'        => $site_key,
            'expectedAction' => 'SIGNUP'
        ]
    ]);

    $opts      = ['http' => ['method' => 'POST', 'header' => 'Content-Type: application/json', 'content' => $body, 'ignore_errors' => true]];
    $rawResult = @file_get_contents($url, false, stream_context_create($opts));
    $result    = ($rawResult !== false) ? json_decode($rawResult, true) : null;

    if (!$result || empty($result['tokenProperties']['valid']) || ($result['riskAnalysis']['score'] ?? 0) < 0.3) {
        $score = $result['riskAnalysis']['score'] ?? 'N/A';
        log_warn('register', "reCAPTCHA failed - score:{$score}");
        echo json_encode(['success' => false, 'message' => 'Security check failed. Please try again.']);
        exit();
    }

    $username = $_POST['username'];
    $email    = $_POST['email'];
    $password = $_POST['password'];

    $stmt = $conn->prepare("SELECT user_id FROM users WHERE username = ?");
    $stmt->bind_param("s", $username);
    $stmt->execute();
    if ($stmt->get_result()->num_rows > 0) {
        $response['success'] = false;
        $response['message'] = "Username already taken";
        log_warn('register', "Username taken: {$username}");
        echo json_encode($response);
        exit();
    }

    $stmt = $conn->prepare("SELECT user_id FROM users WHERE email = ?");
    $stmt->bind_param("s", $email);
    $stmt->execute();
    if ($stmt->get_result()->num_rows > 0) {
        $response['success'] = false;
        $response['message'] = "Email already registered";
        log_warn('register', "Email already registered: {$email}");
        echo json_encode($response);
        exit();
    }

    $hashed_password = password_hash($password, PASSWORD_DEFAULT);

    $stmt = $conn->prepare("INSERT INTO users (username, email, password) VALUES (?, ?, ?)");
    $stmt->bind_param("sss", $username, $email, $hashed_password);

    if ($stmt->execute()) {
        $response['success'] = true;
        $response['message'] = "Registration successful";
        log_info('register', "New user registered - username:{$username} email:{$email}");
    } else {
        $response['success'] = false;
        $response['message'] = "Registration failed";
        log_error('register', "DB insert failed for username:{$username}");
    }
} else {
    $response['success'] = false;
    $response['message'] = "Invalid request method";
}

echo json_encode($response);
?>
