<?php
header('Content-Type: application/json');
require_once 'config.php';

$response = array();

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    // Handle base64 encoded image from Android app
    if (isset($_POST['image']) && !empty($_POST['image'])) {
        $type = isset($_POST['type']) ? $_POST['type'] : 'tweet';
        
        // Decode base64 image
        $image_data = base64_decode(preg_replace('#^data:image/\w+;base64,#i', '', $_POST['image']));
        
        // Set upload directory based on type
        if ($type === 'tweet') {
            $upload_dir = '../images/tweets/';
        } else {
            $upload_dir = '../uploads/';
        }
        
        if (!file_exists($upload_dir)) {
            mkdir($upload_dir, 0755, true);
        }
        
        // Generate unique filename
        $filename = uniqid() . '.jpg';
        $upload_path = $upload_dir . $filename;
        
        if (file_put_contents($upload_path, $image_data) !== false) {
            chmod($upload_path, 0644);
            $response['success'] = true;
            $response['filename'] = $filename;
            if ($type === 'tweet') {
                $response['url'] = 'https://tweeterclone.com.pl/images/tweets/' . $filename;
            } else {
                $response['url'] = 'https://tweeterclone.com.pl/uploads/' . $filename;
            }
        } else {
            $response['success'] = false;
            $response['message'] = 'Failed to save image';
        }
    }
    // Handle file upload (original functionality)
    else if (isset($_FILES['image'])) {
        $file = $_FILES['image'];
        $extension = pathinfo($file['name'], PATHINFO_EXTENSION);
        $filename = uniqid() . '.' . $extension;
        $upload_dir = '../uploads/';
        
        if (!file_exists($upload_dir)) {
            mkdir($upload_dir, 0777, true);
        }
        
        if (move_uploaded_file($file['tmp_name'], $upload_dir . $filename)) {
            $response['success'] = true;
            $response['filename'] = $filename;
            $response['url'] = 'https://tweeterclone.com.pl/uploads/' . $filename;
        } else {
            $response['success'] = false;
            $response['message'] = 'Failed to upload image';
        }
    } else {
        $response['success'] = false;
        $response['message'] = 'No image provided';
    }
} else {
    $response['success'] = false;
    $response['message'] = 'Invalid request method';
}

echo json_encode($response);
?>
