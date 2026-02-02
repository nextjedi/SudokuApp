import { useEffect } from "react";
import { Platform } from "react-native";
import { useDispatch, useSelector } from "react-redux";
import { RootState } from "../store/store";
import { selectCell } from "../slices/gameSlice";

interface UseKeyboardSupportProps {
  onNumberPress: (num: number) => void;
  onErase: () => void;
}

export const useKeyboardSupport = ({
  onNumberPress,
  onErase,
}: UseKeyboardSupportProps) => {
  const dispatch = useDispatch();
  const game = useSelector((state: RootState) => state.game);

  useEffect(() => {
    if (Platform.OS !== "web") return;

    const handleKeyDown = (e: KeyboardEvent) => {
      // Number keys 1-9
      if (e.key >= "1" && e.key <= "9") {
        onNumberPress(parseInt(e.key, 10));
      }
      // Delete/Backspace to erase
      else if (e.key === "Backspace" || e.key === "Delete") {
        onErase();
      }
      // Arrow keys for navigation
      else if (e.key.startsWith("Arrow") && game.selectedCell) {
        e.preventDefault();
        const { row, col } = game.selectedCell;
        let newRow = row;
        let newCol = col;

        switch (e.key) {
          case "ArrowUp":
            newRow = Math.max(0, row - 1);
            break;
          case "ArrowDown":
            newRow = Math.min(8, row + 1);
            break;
          case "ArrowLeft":
            newCol = Math.max(0, col - 1);
            break;
          case "ArrowRight":
            newCol = Math.min(8, col + 1);
            break;
        }

        if (newRow !== row || newCol !== col) {
          dispatch(selectCell({ row: newRow, col: newCol }));
        }
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [game.selectedCell, onNumberPress, onErase, dispatch]);
};
