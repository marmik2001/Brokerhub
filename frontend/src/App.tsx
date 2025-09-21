import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import Login from "./components/ZerodhaLogin";
import ZerodhaCallback from "./pages/ZerodhaCallback";
import Portfolio from "./pages/Portfolio";

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/zerodhaLogin" element={<Login />} />
        <Route path="/redirect" element={<ZerodhaCallback />} />
        <Route path="/portfolio" element={<Portfolio />} />
      </Routes>
    </Router>
  );
}

export default App;
