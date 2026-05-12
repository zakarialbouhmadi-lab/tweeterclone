<?php
header('Content-Type: application/json');
require_once 'config.php';

$response = array();

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    $follower_id = isset($_POST['follower_id']) ? (int)$_POST['follower_id'] : 0;
    $following_id = isset($_POST['following_id']) ? (int)$_POST['following_id'] : 0;
    $action = isset($_POST['action']) ? $_POST['action'] : '';

    if ($follower_id == 0 || $following_id == 0 || !in_array($action, ['accept', 'decline'])) {
        $response['success'] = false;
        $response['message'] = "Invalid parameters";
        echo json_encode($response);
        exit();
    }

    try {
        if ($action == 'accept') {
            $stmt = $conn->prepare("UPDATE follows SET status = 'accepted' WHERE follower_id = ? AND following_id = ? AND status = 'pending'");
            $stmt->bind_param("ii", $follower_id, $following_id);

            if ($stmt->execute() && $stmt->affected_rows > 0) {
                $response['success'] = true;
                $response['message'] = "Follow request accepted";
                log_info('follow_request', "Accepted - follower_id:{$follower_id} following_id:{$following_id}");
            } else {
                $response['success'] = false;
                $response['message'] = "No pending request found";
                log_warn('follow_request', "Accept failed (no pending) - follower_id:{$follower_id} following_id:{$following_id}");
            }
        } else {
            $stmt = $conn->prepare("DELETE FROM follows WHERE follower_id = ? AND following_id = ? AND status = 'pending'");
            $stmt->bind_param("ii", $follower_id, $following_id);

            if ($stmt->execute() && $stmt->affected_rows > 0) {
                $response['success'] = true;
                $response['message'] = "Follow request declined";
                log_info('follow_request', "Declined - follower_id:{$follower_id} following_id:{$following_id}");
            } else {
                $response['success'] = false;
                $response['message'] = "No pending request found";
                log_warn('follow_request', "Decline failed (no pending) - follower_id:{$follower_id} following_id:{$following_id}");
            }
        }
    } catch (Exception $e) {
        $response['success'] = false;
        $response['message'] = "Error: " . $e->getMessage();
        log_error('follow_request', "Exception - follower_id:{$follower_id} following_id:{$following_id} msg:" . $e->getMessage());
    }
} else {
    $response['success'] = false;
    $response['message'] = "Invalid request method";
}

echo json_encode($response);
?>
