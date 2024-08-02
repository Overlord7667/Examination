package com.betelgeuse.corp.examination.add_work

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.betelgeuse.corp.examination.R
import com.betelgeuse.corp.examination.dao.PhotoDao
import com.betelgeuse.corp.examination.dao.PhotoEntity
import com.betelgeuse.corp.examination.listwork.ListWork
import com.google.common.util.concurrent.ListenableFuture
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class AddFileActivity : AppCompatActivity() {
    private lateinit var imageView: ImageView
    private lateinit var imageView2: ImageView
    private val PICK_IMAGE_REQUEST = 1
    private val REQUEST_IMAGE_CAPTURE = 2
    private val REQUEST_COMMENT = 4
    private var selectedImageUri: Uri? = null
    private var selectedImageUri2: Uri? = null
    private var currentImageIndex = 0
    private lateinit var textBuildSP: TextView
    private val SELECT_ITEM_REQUEST = 3

    private val CAMERA_PERMISSION_REQUEST_CODE = 100
    private val ADD_FILE_REQUEST_CODE = 10

    private lateinit var spinnerTypeRoom: Spinner
    private lateinit var spinnerTypeObject: Spinner
    private lateinit var spinnerTypeWork: Spinner

    private lateinit var commentEditText: EditText
    private var photoId: Int = -1

    private var currentPhotoPath: String = ""
    private var currentComment: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_file)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        imageView = findViewById(R.id.imageViewID)
        imageView2 = findViewById(R.id.imageViewID2)
        textBuildSP = findViewById(R.id.textBuildSP)

        commentEditText = findViewById(R.id.commentID)

        spinnerTypeRoom = findViewById(R.id.spinnerTypeRoom)
        spinnerTypeObject = findViewById(R.id.spinnerObjectRoom)
        spinnerTypeWork = findViewById(R.id.spinnerTypeWork)

        // Получаем переданные данные из Intent
        photoId = intent.getIntExtra("photo_id", -1)
        val photoComment = intent.getStringExtra("photo_comment")
        val imageUriString = intent.getStringExtra("imageUri")
        val imageUri2String = intent.getStringExtra("imageUri2")
        val buildSpText = intent.getStringExtra("buildSpText")
        val selectedTypeRoom = intent.getStringExtra("selectedTypeRoom")
        val selectedTypeObject = intent.getStringExtra("selectedTypeObject")
        val selectedTypeWork = intent.getStringExtra("selectedTypeWork")

        // Устанавливаем полученные данные в элементы интерфейса
        commentEditText.setText(photoComment)
        textBuildSP.text = buildSpText

        imageUriString?.let {
            selectedImageUri = Uri.parse(it)
            imageView.setImageURI(selectedImageUri)
            imageView.visibility = View.VISIBLE
        }
        imageUri2String?.let {
            selectedImageUri2 = Uri.parse(it)
            imageView2.setImageURI(selectedImageUri2)
            imageView2.visibility = View.VISIBLE
        }

        setSpinnerSelection(spinnerTypeRoom, selectedTypeRoom)
        setSpinnerSelection(spinnerTypeObject, selectedTypeObject)
        setSpinnerSelection(spinnerTypeWork, selectedTypeWork)

        val addImageButton: Button = findViewById(R.id.addImageID)
        addImageButton.setOnClickListener {
            showImageSourceOptions()
        }

        val cancelButton: Button = findViewById(R.id.cancelButton)
        cancelButton.setOnClickListener {
            finish()
        }

        val addFileButton: Button = findViewById(R.id.addFileInFile)
        addFileButton.setOnClickListener {
            val comment = commentEditText.text.toString()
            val imageUriString = selectedImageUri?.toString()
            val imageUriString2 = selectedImageUri2?.toString()
            val buildSpText = textBuildSP.text.toString()
            val selectedTypeRoom = spinnerTypeRoom.selectedItem.toString()
            val selectedTypeObject = spinnerTypeObject.selectedItem.toString()
            val selectedTypeWork = spinnerTypeWork.selectedItem.toString()

            val resultIntent = Intent().apply {
                putExtra("comment", if (comment.isNotEmpty()) comment else currentComment)
                putExtra("imageUri", imageUriString)
                putExtra("imageUri2", imageUriString2)
                putExtra("buildSpText", buildSpText)
                putExtra("selectedTypeRoom", selectedTypeRoom)
                putExtra("selectedTypeObject", selectedTypeObject)
                putExtra("selectedTypeWork", selectedTypeWork)
            }

            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }

//        val saveButton: Button = findViewById(R.id.addFileInFile)
//        saveButton.setOnClickListener {
//            val updatedComment = commentEditText.text.toString()
//            val resultIntent = Intent().apply {
//                putExtra("photo_id", photoId)
//                putExtra("updated_comment", updatedComment)
//            }
//            setResult(Activity.RESULT_OK, resultIntent)
//            finish()
//        }

        val addSPButton: Button = findViewById(R.id.addSp)
        addSPButton.setOnClickListener {
            val intent = Intent(this, SP_Build::class.java)
            startActivityForResult(intent, SELECT_ITEM_REQUEST)
        }

        val addDefectButton: Button = findViewById(R.id.addDefectButton)
        addDefectButton.setOnClickListener {
            showAddDefectDialog()
        }

        setupSpinnerTypeRoom()
        setupSpinnerTypeObject()
        setupSpinnerTypeWork()
        setupSpinnerProblem()

        spinnerTypeObject.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedTypeObject = parent.getItemAtPosition(position).toString()
                addSPButton.setOnClickListener {
                    val intent = Intent(this@AddFileActivity, SP_Build::class.java)
                    intent.putExtra("selectedTypeObject", selectedTypeObject)
                    intent.putStringArrayListExtra("dataList", ArrayList(dataMap[selectedTypeObject]))
                    startActivityForResult(intent, SELECT_ITEM_REQUEST)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Ничего не делать
            }
        }
        checkPermissions()
    }

    private fun setSpinnerSelection(spinner: Spinner, selectedItem: String?) {
        val adapter = spinner.adapter
        if (adapter is ArrayAdapter<*>) {
            @Suppress("UNCHECKED_CAST")
            val stringAdapter = adapter as ArrayAdapter<String>

            val position = selectedItem?.let {
                stringAdapter.getPosition(it)
            } ?: -1

            if (position != -1) {
                spinner.setSelection(position, false)
            }
        } else {
            // Handle the case where the adapter is not of type ArrayAdapter<String>
            Log.e("AddFileActivity", "Adapter is not an instance of ArrayAdapter<String>")
        }
    }

    private fun showAddDefectDialog() {
        // Inflate the custom dialog layout
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_defect, null)

        // Find the Spinner, EditText, and Button in the dialog layout
        val spinnerDefects = dialogView.findViewById<Spinner>(R.id.spinnerProblem)
        val editTextCustomDefect = dialogView.findViewById<EditText>(R.id.editTextCustomDefect)
        val buttonAddDefect = dialogView.findViewById<Button>(R.id.buttonAddDefect)

        // Create an adapter for the spinner
        val defects = listOf("Скол", "Вмятина", "Прогар", "Трещина", "Разлом", "Отклонение")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, defects)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDefects.adapter = adapter

        // Create and show the dialog
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Добавить дефект")
        builder.setView(dialogView)
        builder.setPositiveButton("Закрыть", null)

        val dialog = builder.create()
        dialog.show()

        // Handle button click event
        buttonAddDefect.setOnClickListener {
            val selectedDefect = spinnerDefects.selectedItem?.toString()
            val customDefect = editTextCustomDefect.text.toString().trim()

            val defectToAdd = if (customDefect.isNotEmpty()) customDefect else selectedDefect
            if (!defectToAdd.isNullOrEmpty()) {
                addDefectToComment(defectToAdd)
            }

            dialog.dismiss()
        }
    }

    private fun addDefectToComment(defect: String) {
        val currentText = commentEditText.text.toString()
        val newText = if (currentText.isEmpty()) defect else "$currentText\n$defect"
        commentEditText.setText(newText)
    }

    private val dataMap = mapOf(
        "Пол" to listOf("Отклонения поверхности покрытия от " +
                "плоскости при проверке двухметровой " +
                "контрольной рейкой: " +
                "-песчаных, мозаично-бетонных, " +
                "асфальтобетонных, керамических, каменных, " +
                "шлакоситалловых – не более 4 мм. " +
                "Уступы между смежными изделиями покрытий " +
                "из штучных материалов из керамических " +
                "(керамогранитных), каменных, цементно- " +
                "песчаных, мозаично-бетонных, " +
                "шлакоситалловых плит не более 1мм.",

            "7.4.13 Швы облицовки должны быть ровными, " +
                    "одинаковой ширины. Через сутки после " +
                    "твердения или полимеризации материалов " +
                    "(допускается сокращение технологической " +
                    "паузы, если это предусмотрено ПИР или " +
                    "требованием производителя материала клеевой " +
                    "прослойки), применяемых для устройства " +
                    "облицовки, швы должны быть заполнены " +
                    "специальными шовными материалами. Перед " +
                    "началом выполнения работ по заполнению " +
                    "швов облицовки необходимо убедиться в совместимости состава затирки с камнем " +
                    "облицовки.",

            "Трещины, сколы, пятна, потеки " +
                    "мастики на облицованной плитками " +
                    "поверхности не допускаются.",

            "Требования к готовому " +
                    "покрытию пола. " +
                    "- Отклонения от заданного уклона покрытий - " +
                    "не более 0,2% соответствующего размера " +
                    "помещения, но не более 10 мм. " +
                    "Технические рекомендации по технологии " +
                    "устройства покрытия пола из ламинат-паркета " +
                    "ТР 114-01.",

            "п.7.1. Покрытие из ламинат-паркета должно " +
                    "быть плотным. Допускаются отдельные неплотности (зазоры) между досками шириной " +
                    "не более 0,3 мм.",

            "п.7.2. Покрытие пола должно быть ровным, " +
                    "перепады (провесы) между соседними досками " +
                    "не допускаются.",

            "п.7.3. Горизонтальность и ровность " +
                    "поверхности покрытия пола проверяют " +
                    "уровнем и контрольной 2-х метровой рейкой. " +
                    "Величина просвета между рейкой и покрытием " +
                    "при проверке в любом направлении не должна " +
                    "превышать 2 мм. " +
                    "ГОСТ 32304-2013 Ламинированные напольные " +
                    "покрытия на основе древесноволокнистых плит " +
                    "сухого способа производства. Технические " +
                    "условия:",

            "8.1 Внешний вид ламинированных напольных " +
                    "покрытий контролируют визуально без " +
                    "применения увеличительных приборов и/или с " +
                    "помощью электронного средства контроля. Для " +
                    "элементов ламинированных напольных " +
                    "покрытий не допускаются выкрашивание углов " +
                    "и сколы кромок по периметру поверхностного " +
                    "слоя, дефекты поверхности. Качество " +
                    "поверхности определяют путем сопоставления " +
                    "с образцом-эталоном."
            ),

        "Стена" to listOf("7.6.1 Перед началом проведения обойных работ " +
                "необходимо провести подготовку основания в " +
                "соответствии с требованиями, " +
                "представленными в таблице 7.2. Качество " +
                "поверхности, подготовленной для оклейки " +
                "обоями, должно соответствовать требованиям, " +
                "приведенным в таблице 7.5, в соответствии с " +
                "выбранным типом обоев.",

            "7.6.15 Приемку работ проводят путем " +
                    "визуального осмотра. При визуальном осмотре " +
                    "на поверхности, оклеенной обоями, не " +
                    "допускают воздушные пузыри, замятины, пятна " +
                    "и другие загрязнения, а также доклейки и " +
                    "отслоения.",

            "7.2.13 Качество производства штукатурных n" +
                    "работ оценивают согласно требованиям, " +
                    "представленным в таблице 7.4. Категорию " +
                    "качества поверхности устанавливают проектом " +
                    "и оценивают согласно таблице 7.5. Категории " +
                    "качества поверхности К3 и К4 устанавливают " +
                    "только для высококачественной штукатурки. " +
                    "Таблица 7.4: " +
                    "Высококачественная штукатурка: " +
                    "Отклонение от вертикали - не более 0,5 мм на 1 " +
                    "м, но не более 5 мм на всю высоту помещения. " +
                    "Допускается наличие следов от абразива, " +
                    "применяемого при шлифовке поверхности, но " +
                    "не глубже 0,3 мм (сплошной визуальный " +
                    "осмотр). " +
                    "Тени от бокового света допускаются, но они " +
                    "должны быть значительно меньше, чем при " +
                    "качестве поверхности категории К2 (контроль " +
                    "проводят при необходимости)",

            "7.3.7 После проведения штукатурных и (или) " +
                    "шпатлевочных отделочных работ качество " +
                    "полученной поверхности должно " +
                    "соответствовать требованиям заказчика. " +
                    "Рекомендуемые параметры приведены в " +
                    "Таблица 7.4 СП 71.13330.2017 Изоляционные и отделочные покрытия. Актуализированная \" +\n" +
                    "редакция СНиП 3.04.01-87.",

            "7.4.17 При производстве облицовочных работ " +
                    "должны быть соблюдены требования, " +
                    "представленные в таблице 7.6. " +
                    "Таблица 7.6 – Требования к облицовочным " +
                    "покрытиям: " +
                    "Для керамических, стеклокерамических, " +
                    "других изделий при внутренней облицовке " +
                    "отклонение от вертикали на 1 м длины не " +
                    "более – 1,5 мм.",

            "Допускается наличие следов от абразива, " +
                    "применяемого при шлифовке поверхности, но " +
                    "не глубже 0,3 мм (сплошной визуальный " +
                    "осмотр). Тени от бокового света допускаются, но они " +
                    "должны быть значительно меньше, чем при " +
                    "качестве поверхности категории К2 (контроль " +
                    "проводят при необходимости).",

            "п. 7.6.15 При визуальном осмотре на " +
                    "поверхности, оклеенной обоями, не допускают " +
                    "воздушные пузыри, замятины, пятна и другие " +
                    "загрязнения, а также доклейки и отслоения",

            "Не допускаются полосы, пятна, подтеки, " +
                    "брызги",

            "7.4.13 Швы облицовки должны быть ровными, " +
                    "одинаковой ширины. Через сутки после " +
                    "твердения или полимеризации материалов (допускается сокращение технологической " +
                    "паузы, если это предусмотрено ПИР или " +
                    "требованием производителя материала клеевой " +
                    "прослойки), применяемых для устройства " +
                    "облицовки, швы должны быть заполнены " +
                    "специальными шовными материалами. Перед " +
                    "началом выполнения работ по заполнению " +
                    "швов облицовки необходимо убедиться в " +
                    "совместимости состава затирки с камнем " +
                    "облицовки.",

            "Трещины, сколы, пятна, потеки " +
                    "мастики на облицованной плитками " +
                    "поверхности не допускаются.",

            ),

        "Потолок" to listOf("п. 7.8.2 Поверхность натяжного потолка должна " +
                "иметь однородный цвет, быть ровной, без " +
                "складок, разрывов, трещин, следов и " +
                "отпечатков использованных материалов. Не " +
                "должно быть щелей между стенами и потолком.",

            "7.8.3 В местах расположения осветительных " +
                    "приборов (люстр, точечных светильников и " +
                    "пр.), вентиляционных решеток и других местах, " +
                    "где необходимо устройство отверстий по " +
                    "контуру отверстия, следует наклеивать на " +
                    "внутреннюю сторону полотна термокольцо для " +
                    "усиления материала. Разрезы в месте прохода " +
                    "труб отопления должны быть полностью " +
                    "закрыты декоративными пластиковыми " +
                    "обводами."
        ),

        "Дверь" to listOf("5.1.5 Конструкция дверных блоков должна " +
                "обеспечивать их безотказное открывание и " +
                "закрывание в течение всего срока " +
                "эксплуатации. Количество циклов открывания " +
                "и закрывания указывают в паспорте изделия.",

            "5.1.5 Конструкция дверных блоков должна " +
                    "обеспечивать их безотказное открывание и " +
                    "закрывание в течение всего срока " +
                    "эксплуатации. Количество циклов открывания " +
                    "и закрывания указывают в паспорте изделия.",

            "5.3.3 Предельные отклонения сборочных " +
                    "единиц и деталей дверных блоков не должны " +
                    "превышать значений, приведенных в таблице 4.",

            "5.3.4 Отклонения от плоскостности и " +
                    "прямолинейности сторон дверных блоков и их " +
                    "сборочных единиц не должны превышать, мм, " +
                    "по высоте, ширине и диагонали элементов: " +
                    "- до 1000 мм.................................................1.0; " +
                    "- св. 1000 до 1600 мм......................................1.0; " +
                    "- св. 1600 до 2500 мм......................................2.0.",

            "5.3.5 Перепад лицевых поверхностей (провес) в " +
                    "соединениях коробок и полотен, установка " +
                    "которых предусмотрена в одной плоскости, не " +
                    "должен превышать 0,7 мм.",

            "5.3.7 Зазоры в местах неподвижных соединений " +
                    "элементов дверных блоков не должны быть " +
                    "более 0,3 мм.",

            "5.4.8 Установка и крепление наличников, " +
                    "доборных элементов, нащельников, обкладок, " +
                    "реек, раскладок и других элементов облицовки " +
                    "и отделки должны обеспечивать надежное " +
                    "соединение с сопрягаемыми элементами " +
                    "проема и конструкции дверного блока под " +
                    "действием нагрузок, возникающих при " +
                    "нормальных условиях эксплуатации. " +
                    "Наличники и доборные элементы должны " +
                    "полностью перекрывать монтажные швы.",

            "5.5.4.5 На лицевых поверхностях деталей " +
                    "дверных блоков под прозрачное отделочное " +
                    "покрытие не допускаются пороки и дефекты " +
                    "обработки древесины, за исключением " +
                    "завитков, свилеватости, крени, тяговой " +
                    "древесины, глазков, трещин шириной до 0,1 мм, " +
                    "а также наклон волокон и здоровых сросшихся " +
                    "и частично сросшихся сучков, допускаемых нормами ограничений, приведенных в таблице " +
                    "В.1, приложение В. " +
                    "Ожог и царапины на лицевых поверхностях " +
                    "деталей не допускаются.",

            "5.6.7 Клеевые материалы, применяемые при " +
                    "облицовке дверных блоков, должны " +
                    "обеспечивать достаточную прочность " +
                    "сцепления, при этом не допускаются не " +
                    "проклеенные участки, складки, волнистость и " +
                    "другие дефекты внешнего вида. Прочность " +
                    "сцепления декоративного отделочного " +
                    "покрытия с изделием должна быть не менее 2,5 " +
                    "Н/мм.",

            "Г.6 Дверные блоки следует устанавливать по " +
                    "уровню и отвесу. Отклонение от вертикали и " +
                    "горизонтали профилей коробок " +
                    "смонтированных изделий не должно" +
                    "превышать 1,5 мм на 1 м длины, но не более 3 " +
                    "мм на высоту изделия. В случае если " +
                    "противоположные профили отклонены в " +
                    "разные стороны (\"скручивание\" коробки), их суммарное отклонение от нормали не должно " +
                    "превышать 3 мм.\n" +
            "К стальным и деревянным дверным блокам" +
                    "применяются аналогичные требования в части" +
                    "недопустимости уклона дверного проема.",

            "п.5.3.9 Усилие, прикладываемое к дверному " +
                    "полотну при закрывании до требуемого сжатия " +
                    "уплотняющих прокладок, в зависимости от " +
                    "массы полотна не должно превышать 120 Н, " +
                    "при этом в закрытом положении защелка и " +
                    "засов замка должны работать без заеданий. " +
                    "Усилие, необходимое для открывания дверного " +
                    "полотна, не должно превышать 75 Н " +
                    "(эргономические требования), для дверных " +
                    "блоков группы В - 50 Н.",

            "п.5.3.11 Внешний вид изделий: цвет, " +
                    "допустимые дефекты поверхности " +
                    "облицовочных материалов и окрашенных " +
                    "элементов (риски, царапины и др.) должен " +
                    "соответствовать образцам-эталонам, " +
                    "утвержденным руководителем предприятия- " +
                    "изготовителя.\n" +
                    "Различия в цвете, глянце и дефекты " +
                    "поверхности, видимые невооруженным глазом с расстояния 0,6-0,8 м при естественном " +
                    "освещении не менее 300 лк, не допускаются.\n" +
                    "ГОСТ 475-2016 \"Блоки дверные деревянные и " +
                    "комбинированные. Общие технические " +
                    "условия\"",

            "5.6.4 Лицевые поверхности дверных блоков не " +
                    "должны иметь трещин, заусенцев, " +
                    "механических повреждений. Требования к " +
                    "лицевым поверхностям устанавливают в " +
                    "технической документации изготовителя и/или " +
                    "в договорах на поставку.",

            "5.6.7 Клеевые материалы, применяемые при " +
                    "облицовке дверных блоков, должны " +
                    "обеспечивать достаточную прочность " +
                    "сцепления, при этом не допускаются не " +
                    "проклеенные участки, складки, волнистость и " +
                    "другие дефекты внешнего вида. Прочность " +
                    "сцепления декоративного отделочного " +
                    "покрытия с изделием должна быть не менее 2,5 " +
                    "Н/мм.",
        ),

        "Окно" to listOf("ГОСТ 30674-99. Блоки оконные из " +
                "поливинилхлоридных профилей. Технические " +
                "условия. Г.6 Оконные блоки следует устанавливать по " +
                "уровню. Отклонение от вертикали и " +
                "горизонтали сторон коробок смонтированных " +
                "изделий не должны превышать 1,5 мм на 1 м " +
                "длины, но не более 3 мм на высоту изделия.",

            "п.5.3.5 Внешний вид изделий: цвет, глянец, " +
                    "допустимые дефекты поверхности ПВХ " +
                    "профилей (риски, царапины, усадочные " +
                    "раковины и др.) должен соответствовать " +
                    "образцам-эталонам, утвержденным " +
                    "руководителем предприятия - изготовителя " +
                    "изделий.",

            "п.5.8.5 Запирающие приборы должны " +
                    "обеспечивать надежное запирание " +
                    "открывающихся элементов изделий. " +
                    "Открывание и закрывание должно происходить " +
                    "легко, плавно, без заеданий. Ручки и засовы " +
                    "приборов не должны самопроизвольно " +
                    "перемещаться из положения \"открыто\" или " +
                    "\"закрыто\".",

            "5.8.6 Конструкции запирающих приборов и " +
                    "петель должны обеспечивать плотный и " +
                    "равномерный обжим прокладок по всему " +
                    "контуру уплотнения в притворах.",

            "Г.2.1 Места примыкания накладных " +
                    "внутренних откосов (независимо от их " +
                    "конструкции) к коробке оконного блока и " +
                    "монтажному шву должны быть " +
                    "герметизированы, при этом должны " +
                    "выполняться мероприятия, исключающие в " +
                    "период эксплуатации проявление трещин и " +
                    "щелей (например, уплотнение примыканий " +
                    "герметиками или другими материалами, " +
                    "обладающими достаточной деформационной " +
                    "устойчивостью).",

            "9.4 При проектировании и исполнении узлов " +
                    "примыкания должны выполняться следующие " +
                    "условия:\n" +
                    "заделка монтажных зазоров между изделиями и " +
                    "откосами проемов стеновых конструкций " +
                    "должна быть плотной, герметичной, " +
                    "рассчитанной на выдерживание климатических " +
                    "нагрузок снаружи и условий эксплуатации " +
                    "внутри помещений; …герметизация швов со стороны помещений должна быть более " +
                    "плотной, чем снаружи»",

            "Г.4 При проектировании и исполнении узлов " +
                    "примыкания должны выполняться следующие " +
                    "условия: " +
                    "заделка монтажных зазоров между изделиями и " +
                    "откосами проемов стеновых конструкций " +
                    "должна быть по всему периметру окна плотной, " +
                    "герметичной, рассчитанной на выдерживание " +
                    "климатических нагрузок снаружи и условий " +
                    "эксплуатации внутри помещений.",

            "5.9.3 Угловые и Т-образные соединения " +
                    "профилей должны быть герметичными. " +
                    "Допускается уплотнение механических " +
                    "соединений ПВХ профилей " +
                    "атмосферостойкими эластичными " +
                    "прокладками. Зазоры до 0,5 мм допускается " +
                    "заделывать специальными герметиками, не " +
                    "ухудшающими внешний вид изделий и " +
                    "обеспечивающими защиту соединений от " +
                    "проникновения влаги.",

            "п.5.3.2 На поверхности профиля- " +
                    "полуфабриката не должно быть следов " +
                    "расслоений, неметаллических и металлических " +
                    "включений, коррозионных пятен и раковин, " +
                    "кратеров. \n" +
                    "На поверхности профиля-полуфабриката не " +
                    "допускаются механические повреждения, " +
                    "плёны, пузыри величиной более 0,07 мм, " +
                    "продольные следы от матрицы, задиры и " +
                    "налипы размером более 0,03 мм, а также " +
                    "поперечные следы от матрицы, образующиеся " +
                    "при остановке пресса.\n" +
                    "На лицевой поверхности профиля- " +
                    "полуфабриката, указываемой на его чертеже, не " +
                    "допускаются механические повреждения, " +
                    "плёны, пузыри величиной более 0,01 мм, " +
                    "продольные следы от матрицы глубиной более " +
                    "0,005 мм, а также поперечные следы от " +
                    "матрицы, образующиеся при остановке пресса.",

            "п. 7.7- Пороки внешнего вида стекла " +
                    "определяют по нормативным документам на " +
                    "соответствующий вид используемого стекла.\n" +
                    "Чистоту поверхностей стекол в стеклопакетах, " +
                    "щербление края стекла, сколы, выступы края " +
                    "стекла, повреждение углов, непрерывность " +
                    "герметизирующих слоев контролируют " +
                    "визуально при освещенности от 300 до 600 лк " +
                    "на расстоянии от 0,6 до 1,0 м",

            "п. 5.6.17 - Уплотняющие прокладки должны " +
                    "устанавливаться непрерывно по всему " +
                    "периметру притвора створок и стеклопакета. " +
                    "При кольцевой установке стык прокладок " +
                    "должен находиться в верхней части изделия. " +
                    "При установке прокладок со стыками в углах " +
                    "под 45° стыки прокладок следует сваривать или " +
                    "склеивать (кроме прокладок, устанавливаемых " +
                    "в штапиках). Угловые перегибы и сварные " +
                    "стыки уплотняющих прокладок для " +
                    "стеклопакетов не должны иметь выступов " +
                    "(выпираний), вызывающих сосредоточенные " +
                    "нагрузки на стеклопакеты.",

            "п. 5.2.4 2012 - При определении монтажных " +
                    "зазоров необходимо учесть предельное " +
                    "отклонение от размеров коробок оконного " +
                    "блока. Отклонения от вертикали и горизонтали " +
                    "смонтированных оконных блоков не должны " +
                    "превышать 1,5 мм на 1 м длины, но не более 3 " +
                    "мм на высоту изделия.",
        ),

        "Трубы" to listOf("п. 7.5.5 Таблица 7.7. Полосы, пятна, подтеки, " +
                "брызги, следы от кисти или валика, неровности " +
                "не допускаются")
    )

    private fun checkPermissions() {
        val cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        val storagePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)

        if (cameraPermission != PackageManager.PERMISSION_GRANTED || storagePermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                CAMERA_PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Необходимо разрешение на использование камеры", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupSpinnerProblem() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            listOf("Скол", "Вмятина", "Прогар", "Трещина", "Разлом", "Отклонение")
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    }

    private fun setupSpinnerTypeRoom() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            listOf("Спальная", "Коридор", "Холл", "Гостинная", "Санузел 1", "Санузел 2", "Туалет 1", "Туалет 2", "Балкон (Лоджия)")
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTypeRoom.adapter = adapter
    }

    private fun setupSpinnerTypeObject() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            listOf("Пол", "Стена", "Потолок", "Дверь", "Окно", "Трубы")
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTypeObject.adapter = adapter
    }

    private fun setupSpinnerTypeWork() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            listOf("0", "1", "2")
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTypeWork.adapter = adapter
    }

    private fun showImageSourceOptions() {
        val options = arrayOf("Камера", "Галерея")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Выберите источник")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> openCamera()
                1 -> openGallery()
            }
        }
        builder.show()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    private fun openCamera() {
        val photoFile: File? = createImageFile()
        val photoURI: Uri = FileProvider.getUriForFile(this, "${packageName}.provider", photoFile!!)
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
        }
        startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE)

    }

    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                PICK_IMAGE_REQUEST -> {
                    val imageUri = data?.data
                    openCommentActivity(imageUri)
                }
                REQUEST_IMAGE_CAPTURE -> {
                    val imageUri = Uri.fromFile(File(currentPhotoPath))
                    openCommentActivity(imageUri)
                    // Добавляем фото в общедоступную галерею
                    MediaScannerConnection.scanFile(this, arrayOf(imageUri.path), null, null)
                }
                REQUEST_COMMENT -> {
                    val comment = data?.getStringExtra("comment")
                    val imageUriString = data?.getStringExtra("imageUri")
                    val imageUri = Uri.parse(imageUriString)
                    currentComment = comment
                    if (!comment.isNullOrEmpty()) {
                        commentEditText.setText(comment)
                    }
                    handleImageResult(imageUri)
                }
                SELECT_ITEM_REQUEST -> {
                    val selectedItems = data?.getStringArrayListExtra("selectedItems")
                    if (selectedItems != null && selectedItems.isNotEmpty()) {
                        textBuildSP.text = selectedItems.joinToString(";\n\n ")
                    } else {
                        textBuildSP.text = "Выбор не сделан"
                    }
                }

            }
        }
        if (requestCode == ADD_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val comment = data?.getStringExtra("comment") ?: ""
            val imageUri = data?.getStringExtra("imageUri") ?: ""
            val imageUri2 = data?.getStringExtra("imageUri2") ?: ""
            Log.d("AddFileActivity", "Received imageUri: $imageUri, imageUri2: $imageUri2, comment: $comment")
            // Остальной код
        }
    }

    private fun openCommentActivity(imageUri: Uri?) {
        val intent = Intent(this, CommentActivity::class.java).apply {
            putExtra("imageUri", imageUri.toString())
        }
        startActivityForResult(intent, REQUEST_COMMENT)
    }

    private fun handleImageResult(imageUri: Uri?) {
        if (imageUri != null) {
            if (currentImageIndex == 0) {
                selectedImageUri = imageUri
                imageView.setImageURI(selectedImageUri)
                imageView.visibility = View.VISIBLE // Сделать видимым imageView
                currentImageIndex = 1
            } else {
                selectedImageUri2 = imageUri
                imageView2.setImageURI(selectedImageUri2)
                imageView2.visibility = View.VISIBLE // Сделать видимым imageView2
                currentImageIndex = 0
            }
        }
    }

    private fun enableEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
    }
}