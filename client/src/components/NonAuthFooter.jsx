import "./NonAuthFooter.css";
function nonAuthFooter(){
    return(
      <footer className="non-auth-footer">
        <p>&copy; 2026 ParkEase Inc</p>
        <div className="footer-links">
          <a href="#privacy">Chính sách bảo mật</a>
          <a href="#terms">Điều khoản dịch vụ</a>
        </div>
      </footer>
    );
}

export default nonAuthFooter;