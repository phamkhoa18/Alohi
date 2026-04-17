from PIL import Image

def resize_image(input_path, output_path, new_size):
    """
    Giảm kích thước px của ảnh mà không bị cắt xén (giữ nguyên tỷ lệ),
    dùng thuật toán LANCZOS để cho chất lượng ảnh tốt nhất.
    """
    # Mở ảnh gốc
    img = Image.open(input_path)
    
    # In ra kích thước ban đầu để kiểm tra
    print(f"Kích thước gốc: {img.size}")
    
    # Resize (thu nhỏ) ảnh theo kích thước mới
    # Image.Resampling.LANCZOS dùng để giữ lại độ nét khi thu nhỏ
    resized_img = img.resize(new_size, Image.Resampling.LANCZOS)
    
    # Lưu ra file mới
    resized_img.save(output_path)
    print(f"Đã lưu ảnh thu nhỏ tại: {output_path} với kích thước {new_size}")

if __name__ == "__main__":
    # Cách dùng: Thay đổi tên file và kích thước bạn muốn ở đây
    input_file = "alohi_original.png"
    output_file = "alohi_256.png"
    target_size = (256, 256) # thay 256 bằng kích thước bạn muốn (ví dụ: 128, 128)
    
    resize_image(input_file, output_file, target_size)
