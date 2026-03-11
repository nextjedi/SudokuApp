import SwiftUI

struct SudokuGridView: View {
    let grid: SudokuGrid
    let selectedCell: (row: Int, col: Int)?
    let highlightEnabled: Bool
    let solverFillingCell: (Int, Int)?
    let onCellTap: (Int, Int) -> Void

    var body: some View {
        GeometryReader { geometry in
            let size = min(geometry.size.width, geometry.size.height)
            let cellSize = size / 9

            VStack(spacing: 0) {
                ForEach(0..<9, id: \.self) { row in
                    HStack(spacing: 0) {
                        ForEach(0..<9, id: \.self) { col in
                            cellView(row: row, col: col)
                                .frame(width: cellSize, height: cellSize)
                        }
                    }
                }
            }
            .border(Color(hex: "#2C3E50"), width: 2)
        }
        .aspectRatio(1, contentMode: .fit)
    }

    @ViewBuilder
    private func cellView(row: Int, col: Int) -> some View {
        let cell = grid[row][col]
        let isSelected = selectedCell?.row == row && selectedCell?.col == col
        let isFilling = solverFillingCell?.0 == row && solverFillingCell?.1 == col
        let isSameRC = highlightEnabled && selectedCell != nil &&
            (selectedCell!.row == row || selectedCell!.col == col)
        let isSameBox = highlightEnabled && selectedCell != nil &&
            (row / 3 == selectedCell!.row / 3 && col / 3 == selectedCell!.col / 3)

        let bg: Color = {
            if isSelected { return Color.blue.opacity(0.3) }
            if isFilling { return Color.yellow.opacity(0.5) }
            if isSameRC || isSameBox { return Color.blue.opacity(0.07) }
            return Color.white
        }()

        ZStack {
            bg
            if cell.value != 0 {
                Text("\(cell.value)")
                    .font(.system(size: 18, weight: cell.isInitial ? .bold : .regular))
                    .foregroundColor(
                        cell.isSolverFilled ? .blue :
                        cell.isInitial ? Color(hex: "#2C3E50") : Color.blue.opacity(0.8)
                    )
            }
        }
        .border(Color.gray.opacity(0.3), width: 0.5)
        .overlay(
            ZStack {
                // Right thick border at columns 2 and 5
                if col == 2 || col == 5 {
                    Rectangle()
                        .frame(width: 2)
                        .foregroundColor(Color(hex: "#2C3E50"))
                        .frame(maxWidth: .infinity, alignment: .trailing)
                }
                // Bottom thick border at rows 2 and 5
                if row == 2 || row == 5 {
                    Rectangle()
                        .frame(height: 2)
                        .foregroundColor(Color(hex: "#2C3E50"))
                        .frame(maxHeight: .infinity, alignment: .bottom)
                }
            }
            .allowsHitTesting(false)
        )
        .onTapGesture { onCellTap(row, col) }
    }
}

extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let r = Double((int >> 16) & 0xFF) / 255
        let g = Double((int >> 8) & 0xFF) / 255
        let b = Double(int & 0xFF) / 255
        self.init(red: r, green: g, blue: b)
    }
}
