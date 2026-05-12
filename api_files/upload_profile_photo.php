<?php
header('Content-Type: application/json');
require_once 'config.php';
$response = array();

// Define paths relative to the script
$image_dir = '../images/profile/';
if (!file_exists($image_dir)) {
    mkdir($image_dir, 0755, true);
}

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    $user_id = $_POST['user_id'];
    $image = $_POST['image'];

    if (empty($user_id) || empty($image)) {
        $response['success'] = false;
        $response['message'] = "Missing required fields";
        echo json_encode($response);
        exit();
    }

    // Decode and save image
    $image_data = base64_decode(preg_replace('#^data:image/\w+;base64,#i', '', $image));
    $filename = 'profile_' . $user_id . '_' . time() . '.jpg';
    $upload_path = $image_dir . $filename;

    $write_result = file_put_contents($upload_path, $image_data);
    if ($write_result !== false) {
        chmod($upload_path, 0644);
        $stmt = $conn->prepare("UPDATE users SET profile_pic = ? WHERE user_id = ?");
        $stmt->bind_param("si", $filename, $user_id);

        if ($stmt->execute()) {
            $response['success'] = true;
            $response['message'] = "Profile photo updated successfully";
            $response['image_url'] = 'https://tweeterclone.com.pl/images/profile/' . $filename;
            log_info('upload_profile_photo', "Photo updated - user_id:{$user_id} file:{$filename}");
        } else {
            $response['success'] = false;
            $response['message'] = "Error updating database";
            log_error('upload_profile_photo', "DB update failed - user_id:{$user_id} file:{$filename}");
        }
        $stmt->close();
    } else {
        $response['success'] = false;
        $response['message'] = "Error saving image file";
        log_error('upload_profile_photo', "File write failed - user_id:{$user_id} path:{$upload_path}");
    }
} else {
    $response['success'] = false;
    $response['message'] = "Invalid request method";
    log_warn('upload_profile_photo', "Invalid request method: " . $_SERVER['REQUEST_METHOD']);
}

echo json_encode($response);
?>
